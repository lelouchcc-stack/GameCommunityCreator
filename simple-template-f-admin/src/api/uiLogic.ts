import http from "../utils/request";
import type { FlowPayload, FlowVO, LogicAtom, LogicGraph, Preset, PresetPayload } from "../types/uiLogic";

/** 原子库：由代码/种子提供，只读 */
export function listLogicNodes(status?: number) {
    return http.get<LogicAtom[], LogicAtom[]>("/admin/site/logic-nodes", { params: { status } });
}

export function listPresets(status?: number) {
    return http.get<Preset[], Preset[]>("/admin/site/presets", { params: { status } });
}

export function createPreset(data: PresetPayload) {
    return http.post<unknown, unknown>("/admin/site/presets", data);
}

/** 已被页面图引用的预设不会原地改，后端会另起一个新版本 */
export function updatePreset(id: number, data: PresetPayload) {
    return http.put<unknown, unknown>(`/admin/site/presets/${id}`, data);
}

export function deletePreset(id: number) {
    return http.delete<unknown, unknown>(`/admin/site/presets/${id}`);
}

/** 页面逻辑图：一个页面可有多张（init/submit...） */
export function listFlows(pageId: number) {
    return http.get<FlowVO[], FlowVO[]>(`/admin/site/pages/${pageId}/flows`);
}

/** 整图保存（存在则更新）；保存时后端会校验节点/端口类型/无环/$ref/nodeRef */
export function saveFlow(pageId: number, flowKey: string, data: FlowPayload) {
    return http.put<unknown, unknown>(`/admin/site/pages/${pageId}/flows/${flowKey}`, data);
}

export type { LogicGraph };
