/**
 * 预设展开（管理端与用户端同源副本）。
 *
 * 预设不把子图"拼"进父图，而是**运行期递归执行**：预设节点执行时用它自己的子图跑一遍，
 * 把子图的 exports 收成预设节点的输出端口。这样父图里 $ref.<预设节点>.<出参> 天然成立，
 * 也不必重写节点 id 与连线。
 */
import type { LogicGraph, LogicNode } from "./types";
import { presetKey } from "./valueRef";

/** 预设节点的键（code@version）；不是预设节点返回 null */
export function presetKeyOf(node: LogicNode): string | null {
    return node.typeCode === "preset" && node.presetCode ? presetKey(node) : null;
}

/** 按 code@version 取预设子图；取不到返回 null（调用方告警后跳过，不白屏） */
export function resolvePreset(presets: Record<string, LogicGraph>, node: LogicNode): LogicGraph | null {
    const key = presetKeyOf(node);
    if (!key) {
        return null;
    }
    return presets[key] ?? null;
}

/** 图的入口节点 = 没有入边 signal 的节点（预设内部图不带触发，靠调用方驱动入口） */
export function entryNodeIds(graph: LogicGraph): string[] {
    const targeted = new Set(graph.edges.filter((edge) => edge.to.port === "signal").map((edge) => edge.to.node));
    return graph.nodes.filter((node) => !targeted.has(node.id)).map((node) => node.id);
}
