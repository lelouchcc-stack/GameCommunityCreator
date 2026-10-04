/**
 * 图执行器与 Vue 的桥（管理端与用户端同源副本）。
 *
 * 引擎（logic/graphRuntime）刻意不依赖 Vue，运行状态由调用方传入；这里用 reactive 包住状态、
 * 通过 provide/inject 递给渲染器，并在拿到站点配置（flows/presets）后重建引擎——
 * 页面首次渲染时 bundle 还没回来，用重建而不是把 getter 塞进引擎，引擎那边就不用改。
 */
import { reactive, type InjectionKey } from "vue";
import { createGraphRuntime, type GraphRuntime } from "../logic/graphRuntime";
import { emptyState, type EngineState, type GraphHost, type LogicGraph } from "../logic/types";

export interface GraphBridge {
    /** 运行状态（reactive，渲染器读它解析 $ref 与显隐） */
    state: EngineState
    /** 让引擎拿到本次下发的图与被引用的预设 */
    setBundle(flows: Record<string, LogicGraph>, presets: Record<string, LogicGraph>): void
    /** 该组件上是否挂了"点击组件"的图（有则点击走图，没有才回落到简单动作） */
    hasClickGraph(nodeId: number | string): boolean
    triggerEvent(kind: "enter" | "click" | "submit", nodeId?: number | string): Promise<void>
    evaluate(value: unknown, params?: Record<string, any>): unknown
    isVisible(nodeId: number | string): boolean
}

export const GRAPH_RUNTIME_KEY = Symbol("graph-runtime") as InjectionKey<GraphBridge>;

export function createGraphBridge(host: GraphHost): GraphBridge {
    const state = reactive<EngineState>(emptyState());
    let flows: Record<string, LogicGraph> = {};
    let presets: Record<string, LogicGraph> = {};
    let engine: GraphRuntime = createGraphRuntime({ state, host, flows, presets });

    return {
        state,

        setBundle(nextFlows, nextPresets) {
            flows = nextFlows ?? {};
            presets = nextPresets ?? {};
            engine = createGraphRuntime({ state, host, flows, presets });
        },

        hasClickGraph(nodeId) {
            return Object.values(flows).some((graph) =>
                graph.nodes.some(
                    (node) =>
                        node.typeCode === "on.click" &&
                        node.config?.nodeId !== undefined &&
                        String(node.config.nodeId) === String(nodeId),
                ),
            );
        },

        triggerEvent(kind, nodeId) {
            return engine.triggerEvent(kind, nodeId);
        },

        evaluate(value, params) {
            return engine.evaluate(value, params);
        },

        isVisible(nodeId) {
            return engine.isVisible(nodeId);
        },
    };
}
