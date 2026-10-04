<template>
    <div class="flow-editor">
        <div class="flow-editor__bar">
            <el-select
                v-model="currentKey"
                placeholder="选择图"
                style="width: 200px"
                @change="onFlowChange"
            >
                <el-option
                    v-for="flow in flows"
                    :key="flow.flowKey"
                    :label="`${flow.flowName || flow.flowKey}（${flow.flowKey}）`"
                    :value="flow.flowKey"
                />
            </el-select>
            <el-input v-model="newKey" placeholder="新图标识，如 submit" style="width: 170px" />
            <el-button @click="createFlow">新建图</el-button>
            <span class="flow-editor__hint">
                左：原子/预设，点一下加进画布；中：点「输出端口」再点「输入端口」连线；右：节点设置
            </span>
            <el-checkbox v-model="mockLoggedIn" class="flow-editor__mock">试跑时当作已登录</el-checkbox>
            <el-button :disabled="!currentKey" @click="dryRun">试跑</el-button>
            <el-button type="primary" :loading="saving" :disabled="!currentKey" @click="save">保存</el-button>
            <el-button @click="$emit('close')">关闭</el-button>
        </div>

        <div class="flow-editor__body">
            <el-card shadow="never" class="flow-editor__pane flow-editor__pane--palette">
                <template #header><span class="flow-editor__title">原子 / 预设</span></template>
                <div v-for="group in atomGroups" :key="group.kind" class="flow-editor__group">
                    <div class="flow-editor__group-label">{{ group.label }}</div>
                    <div class="flow-editor__chips">
                        <el-tag
                            v-for="atom in group.atoms"
                            :key="atom.typeCode"
                            class="flow-editor__chip"
                            :type="atom.runner === 'api' ? 'success' : 'info'"
                            :title="atom.sourceApi || atom.typeCode"
                            @click="addAtom(atom)"
                        >
                            {{ atom.typeName }}
                        </el-tag>
                    </div>
                </div>
                <div class="flow-editor__group">
                    <div class="flow-editor__group-label">预设（原子团）</div>
                    <div class="flow-editor__chips">
                        <el-tag
                            v-for="preset in presets"
                            :key="preset.id"
                            class="flow-editor__chip"
                            type="warning"
                            :title="`${preset.presetCode}@${preset.version}`"
                            @click="addPresetNode(preset)"
                        >
                            {{ preset.presetName }}
                        </el-tag>
                    </div>
                </div>
            </el-card>

            <el-card shadow="never" class="flow-editor__pane flow-editor__pane--canvas">
                <template #header>
                    <span class="flow-editor__title">画布</span>
                    <span class="flow-editor__hint">
                        {{ graph.nodes.length }} 个节点 / {{ graph.edges.length }} 条连线
                        <template v-if="connecting"> · 正在连 {{ connecting.nodeId }}.{{ connecting.port }}，点目标输入端口完成</template>
                    </span>
                    <el-button v-if="connecting" size="small" @click="connecting = null">取消连线</el-button>
                </template>
                <div class="flow-editor__canvas" :style="{ width: layout.width + 'px', height: layout.height + 'px' }">
                    <svg class="flow-editor__edges" :width="layout.width" :height="layout.height">
                        <path
                            v-for="(edge, index) in graph.edges"
                            :key="index"
                            :d="edgePath(edge)"
                            class="flow-editor__edge"
                            :class="{ 'is-selected': selectedEdge === index }"
                            @click="selectedEdge = index"
                        />
                    </svg>
                    <div
                        v-for="node in graph.nodes"
                        :key="node.id"
                        class="flow-editor__node"
                        :class="{
                            'is-selected': selectedNodeId === node.id,
                            'is-preset': node.typeCode === 'preset',
                        }"
                        :style="{ left: position(node.id).x + 'px', top: position(node.id).y + 'px' }"
                        @click="selectNode(node.id)"
                    >
                        <div class="flow-editor__node-head">
                            <span class="flow-editor__node-name">{{ node.label || node.typeCode }}</span>
                            <span class="flow-editor__node-type">{{ node.typeCode === 'preset' ? `${node.presetCode}@${node.presetVersion}` : node.typeCode }}</span>
                            <el-button link type="danger" size="small" @click.stop="removeNode(node.id)">删</el-button>
                        </div>
                        <div class="flow-editor__node-body">
                            <div class="flow-editor__ports flow-editor__ports--in">
                                <div
                                    v-for="(port, i) in portsOf(node).inputs"
                                    :key="port.key"
                                    class="flow-editor__port"
                                    :class="{ 'is-pending': connecting && connecting.nodeId !== node.id }"
                                    :style="{ top: (52 + i * 20) + 'px' }"
                                    @click.stop="clickInput(node.id, port.key)"
                                >
                                    <span class="flow-editor__port-dot" :class="`is-${port.type}`"></span>
                                    {{ port.key }}:{{ port.type }}
                                </div>
                            </div>
                            <div class="flow-editor__ports flow-editor__ports--out">
                                <div
                                    v-for="(port, i) in portsOf(node).outputs"
                                    :key="port.key"
                                    class="flow-editor__port is-out"
                                    :class="{ 'is-active': connecting && connecting.nodeId === node.id && connecting.port === port.key }"
                                    :style="{ top: (52 + i * 20) + 'px' }"
                                    @click.stop="clickOutput(node.id, port.key)"
                                >
                                    {{ port.key }}:{{ port.type }}
                                    <span class="flow-editor__port-dot" :class="`is-${port.type}`"></span>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div v-if="!graph.nodes.length" class="flow-editor__empty">
                        从左侧点一个原子加进来；「进入页面」「点击组件」这类是触发器，一般的图都从它们开始
                    </div>
                </div>
            </el-card>

            <el-card shadow="never" class="flow-editor__pane flow-editor__pane--inspector">
                <template #header><span class="flow-editor__title">节点设置</span></template>
                <template v-if="selectedNode">
                    <el-form label-width="84px" size="small">
                        <el-form-item label="节点 id">
                            <el-input v-model="selectedNode.id" disabled />
                        </el-form-item>
                        <el-form-item label="说明">
                            <el-input v-model="selectedNode.label" maxlength="30" />
                        </el-form-item>
                        <el-form-item
                            v-for="item in settingsOf(selectedNode)"
                            :key="item.key"
                            :label="item.key"
                        >
                            <el-select
                                v-if="item.type === 'nodeRef'"
                                v-model="selectedNode.config![item.key]"
                                clearable
                                placeholder="选本页组件"
                                style="width: 100%"
                            >
                                <el-option
                                    v-for="component in pageNodes"
                                    :key="component.id"
                                    :label="`${component.typeName}「${component.nodeName || component.id}」`"
                                    :value="component.id"
                                />
                            </el-select>
                            <el-select
                                v-else-if="item.type === 'enum'"
                                v-model="selectedNode.config![item.key]"
                                clearable
                                style="width: 100%"
                            >
                                <el-option v-for="option in item.options" :key="option" :label="option" :value="option" />
                            </el-select>
                            <el-switch
                                v-else-if="item.type === 'boolean'"
                                v-model="selectedNode.config![item.key]"
                            />
                            <el-input-number
                                v-else-if="item.type === 'number'"
                                v-model="selectedNode.config![item.key]"
                                style="width: 100%"
                            />
                            <el-input
                                v-else
                                v-model="selectedNode.config![item.key]"
                                :placeholder="bindingPlaceholder(item)"
                            />
                        </el-form-item>
                    </el-form>
                    <p class="flow-editor__tip">
                        值可以写字面量，也可以写引用：<code>$ref.&lt;节点id&gt;.&lt;端口&gt;</code> 引用上游输出、
                        <code>$runtime.query.k</code> 读路由。
                    </p>

                    <el-divider content-position="left">连线</el-divider>
                    <div v-if="edgesOfNode(selectedNode.id).length" class="flow-editor__edge-list">
                        <div v-for="item in edgesOfNode(selectedNode.id)" :key="item.index" class="flow-editor__edge-row">
                            <span>{{ item.text }}</span>
                            <el-button link type="danger" size="small" @click="removeEdge(item.index)">断开</el-button>
                        </div>
                    </div>
                    <p v-else class="flow-editor__tip">该节点还没有连线</p>
                </template>
                <el-empty v-else description="在画布上点一个节点" :image-size="60" />
            </el-card>
        </div>

        <el-dialog v-model="debugVisible" title="试跑结果" width="820px" top="6vh">
            <el-alert
                type="info"
                :closable="false"
                title="试跑用的是假接口与假登录态（不发真请求），只为看控制流走没走对"
                style="margin-bottom: 12px"
            />
            <div class="flow-editor__debug-cols">
                <div>
                    <div class="flow-editor__subtitle">执行轨迹</div>
                    <el-table :data="trace" size="small" border max-height="320">
                        <el-table-column prop="nodeId" label="节点" width="110" />
                        <el-table-column prop="typeCode" label="原子" width="150" />
                        <el-table-column label="状态" width="80">
                            <template #default="{ row }">
                                <el-tag size="small" :type="row.status === 'ok' ? 'success' : row.status === 'error' ? 'danger' : 'info'">
                                    {{ row.status }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="说明">
                            <template #default="{ row }">{{ row.message || "—" }}</template>
                        </el-table-column>
                    </el-table>
                </div>
                <div>
                    <div class="flow-editor__subtitle">宿主被要求做的事</div>
                    <div class="flow-editor__calls">
                        <div v-for="(call, index) in runCalls" :key="index">{{ call }}</div>
                        <span v-if="!runCalls.length" class="flow-editor__tip">什么都没发生</span>
                    </div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { listFlows, listLogicNodes, listPresets, saveFlow } from "../../../api/uiLogic";
import { portsOfNode, settingsOfNode } from "../../../blocks/portSchema";
import { createGraphRuntime } from "../../../logic/graphRuntime";
import type { TraceItem } from "../../../logic/types";
import type { LogicAtom, LogicEdge, LogicGraph, LogicNode, LogicPort, Preset } from "../../../types/uiLogic";

const props = defineProps<{
    pageId: number
    /** 本页组件，供 nodeRef 型设置项挑选 */
    pageNodes: Array<{ id: number; typeName: string; nodeName: string }>
}>();

defineEmits<{ (e: "close"): void }>();

const NODE_W = 220;
const NODE_H = 156;
const GAP_X = 90;
const GAP_Y = 30;
const PORT_TOP = 52;
const PORT_ROW = 20;

const atoms = ref<LogicAtom[]>([]);
const presets = ref<Preset[]>([]);
const flows = ref<Array<{ flowKey: string; flowName: string | null; graph: LogicGraph }>>([]);
const currentKey = ref("");
const newKey = ref("");
const graph = ref<LogicGraph>({ nodes: [], edges: [], exports: {} });
const selectedNodeId = ref<string | null>(null);
const selectedEdge = ref<number | null>(null);
const connecting = ref<{ nodeId: string; port: string } | null>(null);
const saving = ref(false);

const mockLoggedIn = ref(false);
const debugVisible = ref(false);
const trace = ref<TraceItem[]>([]);
const runCalls = ref<string[]>([]);

const selectedNode = computed(() => graph.value.nodes.find((node) => node.id === selectedNodeId.value) ?? null);

const atomGroups = computed(() => {
    const labels: Record<string, string> = {
        trigger: "触发", source: "数据源", operator: "算子", condition: "条件", action: "动作", ui: "界面动作",
    };
    return Object.entries(labels)
        .map(([kind, label]) => ({ kind, label, atoms: atoms.value.filter((atom) => atom.kind === kind) }))
        .filter((group) => group.atoms.length);
});

function portsOf(node: LogicNode): { inputs: LogicPort[]; outputs: LogicPort[] } {
    return portsOfNode(node, atoms.value, presets.value);
}

/** 节点可填的设置项；顺带把没填过的键补成空串，否则 v-model 写不进去 */
function settingsOf(node: LogicNode): LogicPort[] {
    const items = settingsOfNode(node, atoms.value, presets.value);
    for (const item of items) {
        if (node.config?.[item.key] === undefined) {
            node.config = { ...(node.config ?? {}), [item.key]: "" };
        }
    }
    return items;
}

function bindingPlaceholder(item: LogicPort) {
    if (item.type === "text") {
        return "字面量，或 $ref.<节点id>.<端口>";
    }
    return item.type === "number" ? "数字，或 $ref.…" : "字面量或引用";
}

/** 自动布局：按依赖深度分列（设计里明确不做自由绝对定位画布） */
const layout = computed(() => {
    const depth = new Map<string, number>();
    for (const node of graph.value.nodes) {
        depth.set(node.id, 0);
    }
    for (let round = 0; round < graph.value.nodes.length + 1; round++) {
        let changed = false;
        for (const edge of graph.value.edges) {
            const next = (depth.get(edge.from.node) ?? 0) + 1;
            if (next > (depth.get(edge.to.node) ?? 0)) {
                depth.set(edge.to.node, next);
                changed = true;
            }
        }
        if (!changed) {
            break;
        }
    }
    const rows = new Map<number, number>();
    const position = new Map<string, { x: number; y: number }>();
    for (const node of graph.value.nodes) {
        const column = depth.get(node.id) ?? 0;
        const row = rows.get(column) ?? 0;
        rows.set(column, row + 1);
        position.set(node.id, { x: 24 + column * (NODE_W + GAP_X), y: 24 + row * (NODE_H + GAP_Y) });
    }
    const columns = Math.max(...[...rows.keys()].map((key) => key + 1), 1);
    const maxRows = Math.max(...[...rows.values()], 1);
    return {
        position,
        width: 48 + columns * NODE_W + (columns - 1) * GAP_X,
        height: 48 + maxRows * NODE_H + (maxRows - 1) * GAP_Y,
    };
});

function position(nodeId: string) {
    return layout.value.position.get(nodeId) ?? { x: 0, y: 0 };
}

function portIndex(node: LogicNode, port: string, direction: "in" | "out") {
    const ports = direction === "in" ? portsOf(node).inputs : portsOf(node).outputs;
    const index = ports.findIndex((item) => item.key === port);
    return index < 0 ? 0 : index;
}

function edgePath(edge: LogicEdge) {
    const from = graph.value.nodes.find((node) => node.id === edge.from.node);
    const to = graph.value.nodes.find((node) => node.id === edge.to.node);
    if (!from || !to) {
        return "";
    }
    const start = position(from.id);
    const end = position(to.id);
    const x1 = start.x + NODE_W;
    const y1 = start.y + PORT_TOP + portIndex(from, edge.from.port, "out") * PORT_ROW + 8;
    const x2 = end.x;
    const y2 = end.y + PORT_TOP + portIndex(to, edge.to.port, "in") * PORT_ROW + 8;
    return `M ${x1} ${y1} C ${x1 + 60} ${y1}, ${x2 - 60} ${y2}, ${x2} ${y2}`;
}

function selectNode(nodeId: string) {
    selectedNodeId.value = nodeId;
    selectedEdge.value = null;
}

function nextNodeId() {
    // 节点 id 在整页范围内必须唯一（执行器的 outputs 按 nodeId 全局存）。
    // 前缀用图标识但要把非字母数字下划线的字符换掉：$ref 的校验与解析都只认 [A-Za-z0-9_]，
    // 图标识本身允许含 "-"（如 my-flow），直接用会导致绑定写不进 visibleWhen/itemsSource。
    const prefix = currentKey.value.replace(/[^A-Za-z0-9_]/g, "_") || "node";
    let index = graph.value.nodes.length + 1;
    let candidate = `${prefix}_${index}`;
    while (graph.value.nodes.some((node) => node.id === candidate)) {
        candidate = `${prefix}_${++index}`;
    }
    return candidate;
}

function addAtom(atom: LogicAtom) {
    const id = nextNodeId();
    graph.value.nodes.push({ id, typeCode: atom.typeCode, label: atom.typeName, config: {} });
    selectNode(id);
}

function addPresetNode(preset: Preset) {
    const id = nextNodeId();
    graph.value.nodes.push({
        id,
        typeCode: "preset",
        presetCode: preset.presetCode,
        presetVersion: preset.version,
        label: preset.presetName,
        config: {},
    });
    selectNode(id);
}

function removeNode(nodeId: string) {
    graph.value.nodes = graph.value.nodes.filter((node) => node.id !== nodeId);
    graph.value.edges = graph.value.edges.filter((edge) => edge.from.node !== nodeId && edge.to.node !== nodeId);
    if (selectedNodeId.value === nodeId) {
        selectedNodeId.value = null;
    }
}

function removeEdge(index: number) {
    graph.value.edges.splice(index, 1);
    selectedEdge.value = null;
}

function clickOutput(nodeId: string, port: string) {
    connecting.value = { nodeId, port };
}

function clickInput(nodeId: string, port: string) {
    const pending = connecting.value;
    if (!pending) {
        ElMessage.info("先点一个输出端口，再点这里的输入端口");
        return;
    }
    if (pending.nodeId === nodeId) {
        ElMessage.warning("不能连到自己");
        return;
    }
    const from = graph.value.nodes.find((node) => node.id === pending.nodeId);
    const to = graph.value.nodes.find((node) => node.id === nodeId);
    if (!from || !to) {
        return;
    }
    const source = portsOf(from).outputs.find((item) => item.key === pending.port);
    const target = portsOf(to).inputs.find((item) => item.key === port);
    if (!source || !target) {
        return;
    }
    if (source.type !== target.type && target.type !== "any") {
        ElMessage.error(`类型不匹配：${source.type} 接不到 ${target.type}`);
        return;
    }
    if (reaches(nodeId, pending.nodeId)) {
        ElMessage.error("这样连会形成环（本期不支持循环）");
        return;
    }
    // 一个输入端口只接一条边，再连就替换
    graph.value.edges = graph.value.edges.filter((edge) => !(edge.to.node === nodeId && edge.to.port === port));
    graph.value.edges.push({ from: { node: pending.nodeId, port: pending.port }, to: { node: nodeId, port } });
    connecting.value = null;
    ElMessage.success("已连上");
}

/** from 是否可达 target（用来判断新连线会不会成环） */
function reaches(fromNodeId: string, targetNodeId: string) {
    const stack = [fromNodeId];
    const seen = new Set<string>();
    while (stack.length) {
        const current = stack.pop() as string;
        if (current === targetNodeId) {
            return true;
        }
        if (seen.has(current)) {
            continue;
        }
        seen.add(current);
        for (const edge of graph.value.edges) {
            if (edge.from.node === current) {
                stack.push(edge.to.node);
            }
        }
    }
    return false;
}

function edgesOfNode(nodeId: string) {
    return graph.value.edges
        .map((edge, index) => ({ edge, index }))
        .filter((item) => item.edge.from.node === nodeId || item.edge.to.node === nodeId)
        .map((item) => ({
            index: item.index,
            text: `${item.edge.from.node}.${item.edge.from.port} → ${item.edge.to.node}.${item.edge.to.port}`,
        }));
}

function onFlowChange() {
    const flow = flows.value.find((item) => item.flowKey === currentKey.value);
    graph.value = flow
        ? JSON.parse(JSON.stringify(flow.graph ?? { nodes: [], edges: [], exports: {} }))
        : { nodes: [], edges: [], exports: {} };
    if (!graph.value.nodes) {
        graph.value.nodes = [];
    }
    if (!graph.value.edges) {
        graph.value.edges = [];
    }
    selectedNodeId.value = null;
    connecting.value = null;
}

function createFlow() {
    const key = newKey.value.trim();
    if (!/^[a-z][a-z0-9_-]{0,29}$/.test(key)) {
        ElMessage.error("图标识要小写字母开头，后接小写字母/数字/下划线/短横线，最长 30 位");
        return;
    }
    if (flows.value.some((item) => item.flowKey === key)) {
        ElMessage.warning("这个标识已经用过了");
        return;
    }
    newKey.value = "";
    currentKey.value = key;
    graph.value = { nodes: [], edges: [], exports: {} };
    selectedNodeId.value = null;
}

async function save() {
    saving.value = true;
    try {
        await saveFlow(props.pageId, currentKey.value, {
            flowName: currentKey.value,
            graphJson: { nodes: graph.value.nodes, edges: graph.value.edges, exports: graph.value.exports ?? {} },
            status: 1,
            remark: null,
        });
        ElMessage.success("已保存；用户端刷新后按新图执行");
        await loadFlows();
    } catch {
        // 校验不通过时后端会给出具体原因，已由拦截器提示
    } finally {
        saving.value = false;
    }
}

/** 试跑：用假接口 + 假登录态跑这张图，只看控制流走没走对（与用户端同一套引擎） */
async function dryRun() {
    const presetMap: Record<string, LogicGraph> = {};
    for (const preset of presets.value) {
        presetMap[`${preset.presetCode}@${preset.version}`] = preset.graph;
    }
    const calls: string[] = [];
    const host = {
        callApi: async (method: string, path: string, params: Record<string, any>) => {
            calls.push(`接口 ${method} ${path} ${JSON.stringify(params)}`);
            return {
                records: [
                    { id: 1, title: "示例帖 A", versionId: 2, likeCount: 9, status: 1 },
                    { id: 2, title: "示例帖 B", versionId: 1, likeCount: 3, status: 1 },
                ],
                total: 2,
                page: 1,
                size: 10,
                status: 1,
                id: 1,
            };
        },
        navigate: (url: string) => calls.push(`跳转 ${url}`),
        toast: (message: string, type?: string) => calls.push(`提示[${type ?? "info"}] ${message}`),
        scrollTo: (id: number | string) => calls.push(`滚动到 ${id}`),
        setVisible: (id: number | string, visible: boolean) => calls.push(`显隐 ${id}=${visible}`),
        openModal: (id: number | string) => calls.push(`打开弹窗 ${id}`),
        closeModal: (id: number | string) => calls.push(`关闭弹窗 ${id}`),
        componentValue: (id: number | string, field?: string) => `〈组件 ${id} 的 ${field ?? "值"}〉`,
        runtimeValues: () => ({ routeParam: { id: 1 }, query: { k: "x" }, page: 1 }),
        isLoggedIn: () => mockLoggedIn.value,
        currentMemberId: () => (mockLoggedIn.value ? 1 : null),
        logout: () => calls.push("退出登录"),
        onError: (message: string) => calls.push(`出错 ${message}`),
    };
    const state = { outputs: {}, visible: {}, session: {}, loading: {}, trace: [] as TraceItem[] };
    const runtime = createGraphRuntime({ state, host, flows: { [currentKey.value]: graph.value }, presets: presetMap });
    for (const node of graph.value.nodes) {
        if (node.typeCode === "on.pageEnter") {
            await runtime.triggerEvent("enter");
        } else if (node.typeCode === "on.click" && node.config?.nodeId !== undefined) {
            await runtime.triggerEvent("click", node.config.nodeId);
        } else if (node.typeCode === "on.submit" && node.config?.nodeId !== undefined) {
            await runtime.triggerEvent("submit", node.config.nodeId);
        }
    }
    trace.value = [...state.trace];
    runCalls.value = calls;
    debugVisible.value = true;
}

async function loadFlows() {
    const list = await listFlows(props.pageId);
    flows.value = list.map((flow) => ({ flowKey: flow.flowKey, flowName: flow.flowName, graph: flow.graph }));
    if (!currentKey.value && flows.value.length) {
        currentKey.value = flows.value[0]?.flowKey ?? "";
        onFlowChange();
    }
}

onMounted(async () => {
    [atoms.value, presets.value] = await Promise.all([listLogicNodes(1), listPresets(1)]);
    await loadFlows();
});
</script>

<style scoped>
.flow-editor__bar {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 10px;
}

.flow-editor__hint {
    flex: 1;
    font-size: 12px;
    color: #909399;
}

.flow-editor__mock {
    font-size: 12px;
}

.flow-editor__body {
    display: flex;
    gap: 10px;
    align-items: stretch;
}

.flow-editor__pane {
    margin: 0;
}

.flow-editor__pane--palette {
    width: 230px;
    flex-shrink: 0;
    max-height: 640px;
    overflow: auto;
}

.flow-editor__pane--canvas {
    flex: 1;
    min-width: 0;
}

.flow-editor__pane--inspector {
    width: 330px;
    flex-shrink: 0;
    max-height: 640px;
    overflow: auto;
}

.flow-editor__title {
    font-weight: 600;
}

.flow-editor__group {
    margin-bottom: 10px;
}

.flow-editor__group-label {
    font-size: 12px;
    color: #909399;
    margin-bottom: 4px;
}

.flow-editor__chips {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
}

.flow-editor__chip {
    cursor: pointer;
}

.flow-editor__canvas {
    position: relative;
    min-height: 520px;
    background: repeating-linear-gradient(0deg, #fafafa, #fafafa 19px, #f0f0f0 20px),
        repeating-linear-gradient(90deg, #fafafa, #fafafa 19px, #f0f0f0 20px);
    border: 1px dashed var(--el-border-color);
    border-radius: 6px;
    overflow: auto;
}

.flow-editor__edges {
    position: absolute;
    left: 0;
    top: 0;
    pointer-events: none;
}

.flow-editor__edge {
    fill: none;
    stroke: #b1b3b8;
    stroke-width: 2;
    pointer-events: stroke;
    cursor: pointer;
}

.flow-editor__edge.is-selected {
    stroke: var(--el-color-primary);
    stroke-width: 3;
}

.flow-editor__node {
    position: absolute;
    width: 220px;
    height: 156px;
    box-sizing: border-box;
    background: #fff;
    border: 1px solid var(--el-border-color);
    border-radius: 6px;
    box-shadow: 0 1px 4px rgb(0 0 0 / 8%);
    cursor: pointer;
}

.flow-editor__node.is-selected {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 2px rgb(64 158 255 / 25%);
}

.flow-editor__node.is-preset {
    border-style: dashed;
    border-color: #e6a23c;
}

.flow-editor__node-head {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 8px;
    border-bottom: 1px solid #f0f0f0;
}

.flow-editor__node-name {
    font-weight: 600;
    font-size: 13px;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.flow-editor__node-type {
    font-family: monospace;
    font-size: 11px;
    color: #909399;
}

.flow-editor__node-body {
    position: relative;
    height: 108px;
    overflow: hidden;
}

.flow-editor__ports {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 50%;
}

.flow-editor__ports--in {
    left: 0;
}

.flow-editor__ports--out {
    right: 0;
}

.flow-editor__port {
    position: absolute;
    left: 6px;
    right: 6px;
    height: 18px;
    line-height: 18px;
    font-family: monospace;
    font-size: 11px;
    color: #606266;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    border-radius: 4px;
    padding: 0 2px;
}

.flow-editor__port.is-out {
    text-align: right;
}

.flow-editor__port.is-pending:hover {
    background: rgb(64 158 255 / 12%);
    cursor: crosshair;
}

.flow-editor__port.is-active {
    background: rgb(64 158 255 / 20%);
}

.flow-editor__port-dot {
    display: inline-block;
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #c0c4cc;
    vertical-align: middle;
}

.flow-editor__port-dot.is-signal {
    background: #909399;
}

.flow-editor__port-dot.is-listPost,
.flow-editor__port-dot.is-listBoard,
.flow-editor__port-dot.is-listComment,
.flow-editor__port-dot.is-listNotice {
    background: #67c23a;
}

.flow-editor__port-dot.is-boolean {
    background: #e6a23c;
}

.flow-editor__port-dot.is-any {
    background: #b1b3b8;
}

.flow-editor__empty {
    position: absolute;
    left: 24px;
    top: 40px;
    font-size: 13px;
    color: #909399;
}

.flow-editor__tip {
    font-size: 12px;
    color: #909399;
    line-height: 1.7;
}

.flow-editor__tip code {
    background: #f5f7fa;
    padding: 0 3px;
    border-radius: 3px;
}

.flow-editor__edge-list {
    max-height: 180px;
    overflow: auto;
}

.flow-editor__edge-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-family: monospace;
    font-size: 12px;
    line-height: 1.9;
}

.flow-editor__debug-cols {
    display: flex;
    gap: 12px;
}

.flow-editor__debug-cols > div {
    flex: 1;
    min-width: 0;
}

.flow-editor__subtitle {
    font-weight: 600;
    margin-bottom: 6px;
}

.flow-editor__calls {
    max-height: 320px;
    overflow: auto;
    font-size: 12px;
    line-height: 1.9;
    background: #f5f7fa;
    border-radius: 4px;
    padding: 8px;
}
</style>
