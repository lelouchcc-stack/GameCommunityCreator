/**
 * 图执行器（管理端与用户端同源副本）：控制流推 + 数据流拉。
 *
 * 参考 ComfyUI 的惰性求值 + Maa 的触发推进：
 *   - 控制流推：触发器触发后沿 signal 边推进；branch 只走 cond 命中的那一支（短路）
 *   - 数据流拉：某节点执行时若入参来自上游数据节点，就把上游惰性算出来并缓存
 *   - 一次运行内每个节点至多执行一次（computed/pushed 去重），所以图必然终止（无循环）
 *   - 预设节点不拼图，运行期递归跑子图，把 exports 收成它的输出端口
 *   - 作用域：循环容器触发交互时把当前项作为 scope 注入本次运行，图内与渲染层都能读 $item / $index
 *
 * 状态由调用方传入（用户端传 reactive 的对象），引擎本身不依赖 Vue。
 */
import type { EngineState, GraphHost, LogicGraph, LogicNode, TraceItem } from "./types";
import { UNRESOLVED, evalConfig, evalValue, presetKey } from "./valueRef";
import { entryNodeIds, resolvePreset } from "./presetExpand";
import { createExecutors } from "./atomExecutors";

const MAX_TRACE = 200;

export interface GraphRuntimeOptions {
    state: EngineState
    host: GraphHost
    /** 页面图：flowKey -> 图 */
    flows: Record<string, LogicGraph>
    /** 预设定义：code@version -> 子图（后端只下发被引用到的） */
    presets: Record<string, LogicGraph>
}

export interface GraphRuntime {
    /** 跑某个 flow 里的所有"进入页面"根 */
    runFlow(flowKey: string, params?: Record<string, any>): Promise<void>
    /**
     * 组件交互：按 on.click / on.submit 的 nodeId 找到对应根并执行。
     * scope 是循环容器注入的当前项（{ item, index, <别名> }）——列表项里的按钮靠它知道自己点的是哪一条。
     */
    triggerEvent(kind: "enter" | "click" | "submit", componentId?: number | string,
                 scope?: Record<string, any>): Promise<void>
    /** 渲染器解析绑定：$ref / $runtime / $param / $item / $index；解析不出来返回 undefined */
    evaluate(value: unknown, params?: Record<string, any>, scope?: Record<string, any>): unknown
    /** 组件显隐（act.setVisible 写过就按它，没写过按节点自身 visible） */
    isVisible(componentId: number | string): boolean
    reset(): void
}

interface RunContext {
    graph: LogicGraph
    presets: Record<string, LogicGraph>
    prefix: string
    params: Record<string, any>
    /** 本次运行的作用域（循环容器注入的当前项）；页面进入类触发没有 */
    scope?: Record<string, any>
    runId: string
    computed: Set<string>
    running: Set<string>
    pushed: Set<string>
    inputs: Record<string, Record<string, any>>
    errored: Set<string>
    state: EngineState
    host: GraphHost
}

export function createGraphRuntime(options: GraphRuntimeOptions): GraphRuntime {
    const { state, host, flows, presets } = options;
    const executors = createExecutors(host);
    let sequence = 0;

    const nodeKey = (ctx: RunContext, nodeId: string) => ctx.prefix + nodeId;
    const findNode = (graph: LogicGraph, nodeId: string): LogicNode | undefined =>
        graph.nodes.find((node) => node.id === nodeId);

    function trace(ctx: RunContext, node: LogicNode, status: TraceItem["status"], ms: number,
                   inputs?: Record<string, any>, outputs?: Record<string, any>, message?: string) {
        state.trace.unshift({
            runId: ctx.runId,
            nodeId: nodeKey(ctx, node.id),
            typeCode: node.typeCode,
            status,
            ms,
            inputs,
            outputs,
            message,
        });
        if (state.trace.length > MAX_TRACE) {
            state.trace.length = MAX_TRACE;
        }
    }

    /** 解析节点入参：config 先、被边连上的覆盖之；上游没就绪就整体不可用 */
    async function resolveInputs(node: LogicNode, ctx: RunContext): Promise<Record<string, any> | typeof UNRESOLVED> {
        // config 里写了 $ref 的上游先惰性算出来（数据流拉）
        for (const value of Object.values(node.config ?? {})) {
            if (typeof value !== "string" || !value.startsWith("$ref.")) {
                continue;
            }
            const parts = value.split(".");
            const upstream = findNode(ctx.graph, parts[1] ?? "");
            if (upstream && !ctx.computed.has(nodeKey(ctx, upstream.id))) {
                await ensureNode(upstream, ctx);
            }
        }
        const values = evalConfig(node.config, {
            outputs: state.outputs,
            params: ctx.params,
            runtime: host.runtimeValues(),
            // 预设内部节点：outputs 的键带前缀，$ref 要按同一前缀去找（见 EvalContext.prefix）
            prefix: ctx.prefix,
            // 循环容器注入的当前项：列表项里的节点配置可以写 $item.id
            scope: ctx.scope,
        });
        const result: Record<string, any> = values === UNRESOLVED ? {} : { ...values };
        for (const edge of ctx.graph.edges) {
            if (edge.to.node !== node.id || edge.to.port === "signal") {
                continue;
            }
            const upstream = findNode(ctx.graph, edge.from.node);
            if (!upstream) {
                continue;
            }
            await ensureNode(upstream, ctx);
            const value = state.outputs[nodeKey(ctx, upstream.id)]?.[edge.from.port];
            result[edge.to.port] = value === undefined ? UNRESOLVED : value;
        }
        if (values === UNRESOLVED || Object.values(result).includes(UNRESOLVED)) {
            return UNRESOLVED;
        }
        return result;
    }

    /** 预设节点：递归跑子图，把 exports 收成自己的输出端口 */
    async function runPreset(node: LogicNode, ctx: RunContext): Promise<Record<string, any>> {
        const graph = resolvePreset(ctx.presets, node);
        if (!graph) {
            trace(ctx, node, "skipped", 0, undefined, undefined, `未知预设，已跳过：${presetKey(node)}`);
            host.onError?.(`未知预设，已跳过：${presetKey(node)}`);
            return {};
        }
        const inputs = await resolveInputs(node, ctx);
        const params: Record<string, any> = {};
        if (inputs !== UNRESOLVED) {
            for (const [key, value] of Object.entries(inputs)) {
                if (key !== "signal") {
                    params[key] = value;
                }
            }
        }
        const inner: RunContext = {
            ...ctx,
            graph,
            params,
            prefix: `${ctx.prefix}${node.id}/`,
            computed: new Set(),
            running: new Set(),
            pushed: new Set(),
            inputs: {},
            errored: new Set(),
        };
        for (const entryId of entryNodeIds(graph)) {
            const entry = findNode(graph, entryId);
            if (entry) {
                await pushNode(entry, inner);
            }
        }
        const outputs: Record<string, any> = {};
        for (const [name, target] of Object.entries(graph.exports ?? {})) {
            const dot = String(target).indexOf(".");
            if (dot < 0) {
                continue;
            }
            const innerId = String(target).slice(0, dot);
            const port = String(target).slice(dot + 1);
            outputs[name] = inner.state.outputs[inner.prefix + innerId]?.[port];
        }
        trace(ctx, node, "ok", 0, params, outputs, `预设 ${presetKey(node)}`);
        return outputs;
    }

    /** 执行一个节点（不含控制流推进） */
    async function executeNode(node: LogicNode, ctx: RunContext): Promise<Record<string, any>> {
        if (node.typeCode === "preset") {
            return runPreset(node, ctx);
        }
        const executor = executors[node.typeCode];
        if (!executor) {
            trace(ctx, node, "skipped", 0, undefined, undefined, `未知原子，已跳过：${node.typeCode}`);
            host.onError?.(`未知原子，已跳过：${node.typeCode}`);
            return {};
        }
        const inputs = await resolveInputs(node, ctx);
        if (inputs === UNRESOLVED) {
            trace(ctx, node, "skipped", 0, undefined, undefined, "入参还没就绪");
            return {};
        }
        // 落一份解析后的入参：branch 的出口判定要用它读 cond
        ctx.inputs[nodeKey(ctx, node.id)] = inputs;
        const started = Date.now();
        const outputs = await executor({ node, inputs, state, host });
        trace(ctx, node, "ok", Date.now() - started, inputs, outputs);
        return outputs;
    }

    /** 惰性求值：确保某节点算过；失败返回 false（错误已记轨迹并通知宿主） */
    async function ensureNode(node: LogicNode, ctx: RunContext): Promise<boolean> {
        const key = nodeKey(ctx, node.id);
        if (ctx.computed.has(key)) {
            return !ctx.errored.has(key);
        }
        if (ctx.running.has(key)) {
            trace(ctx, node, "skipped", 0, undefined, undefined, "检测到环，已跳过");
            return false;
        }
        ctx.running.add(key);
        state.loading[key] = true;
        try {
            const outputs = await executeNode(node, ctx);
            state.outputs[key] = { ...(state.outputs[key] ?? {}), ...outputs };
            ctx.computed.add(key);
            return true;
        } catch (error) {
            const message = error instanceof Error ? error.message : String(error);
            trace(ctx, node, "error", 0, undefined, undefined, message);
            ctx.errored.add(key);
            ctx.computed.add(key);
            host.onError?.(`${node.label ?? node.typeCode}：${message}`);
            return false;
        } finally {
            state.loading[key] = false;
            ctx.running.delete(key);
        }
    }

    /** 控制流推进：执行节点并把 signal 往下传（每节点一次） */
    async function pushNode(node: LogicNode, ctx: RunContext): Promise<void> {
        const key = nodeKey(ctx, node.id);
        if (ctx.pushed.has(key)) {
            return;
        }
        ctx.pushed.add(key);
        const ok = await ensureNode(node, ctx);
        const outEdges = ctx.graph.edges.filter((edge) => edge.from.node === node.id);
        if (!ok) {
            for (const edge of outEdges.filter((item) => item.from.port === "error")) {
                const next = findNode(ctx.graph, edge.to.node);
                if (next) {
                    await pushNode(next, ctx);
                }
            }
            return;
        }
        const cond = ctx.inputs[key]?.cond;
        for (const edge of outEdges) {
            if (edge.from.port === "error") {
                continue;
            }
            // 分支短路：只走 cond 命中的那一支
            if (node.typeCode === "branch" && edge.from.port !== (cond ? "true" : "false")) {
                continue;
            }
            const next = findNode(ctx.graph, edge.to.node);
            if (next) {
                await pushNode(next, ctx);
            }
        }
    }

    function newContext(graph: LogicGraph, runId: string, params: Record<string, any>,
                        scope?: Record<string, any>): RunContext {
        return {
            graph,
            presets,
            prefix: "",
            params,
            scope,
            runId,
            computed: new Set(),
            running: new Set(),
            pushed: new Set(),
            inputs: {},
            errored: new Set(),
            state,
            host,
        };
    }

    async function runGraphRoots(graph: LogicGraph, runId: string, params: Record<string, any>,
                                 predicate: (node: LogicNode) => boolean,
                                 scope?: Record<string, any>) {
        const ctx = newContext(graph, runId, params, scope);
        for (const node of graph.nodes) {
            if (!predicate(node)) {
                continue;
            }
            await pushNode(node, ctx);
        }
    }

    return {
        async runFlow(flowKey, params = {}) {
            const graph = flows[flowKey];
            if (!graph) {
                return;
            }
            await runGraphRoots(graph, `${flowKey}#${++sequence}`, params, (node) => node.typeCode === "on.pageEnter");
        },

        async triggerEvent(kind, componentId, scope) {
            const targetType = kind === "enter" ? "on.pageEnter" : kind === "click" ? "on.click" : "on.submit";
            for (const [flowKey, graph] of Object.entries(flows)) {
                await runGraphRoots(graph, `${flowKey}#${++sequence}`, {}, (node) => {
                    if (node.typeCode !== targetType) {
                        return false;
                    }
                    if (kind === "enter") {
                        return true;
                    }
                    return node.config?.nodeId !== undefined && String(node.config.nodeId) === String(componentId);
                }, scope);
            }
        },

        evaluate(value, params = {}, scope) {
            const resolved = evalValue(value, {
                outputs: state.outputs,
                params,
                runtime: host.runtimeValues(),
                scope,
            });
            return resolved === UNRESOLVED ? undefined : resolved;
        },

        isVisible(componentId) {
            return state.visible[String(componentId)] !== false;
        },

        reset() {
            state.outputs = {};
            state.visible = {};
            state.loading = {};
            state.trace = [];
            state.session = {};
        },
    };
}
