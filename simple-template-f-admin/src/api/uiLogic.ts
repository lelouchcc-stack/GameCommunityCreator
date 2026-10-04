import http from "../utils/request";
import type { LogicNode, Preset, PresetPayload } from "../types/uiLogic";

/** 原子库：由代码/种子提供，只读 */
export function listLogicNodes(status?: number) {
    return http.get<LogicNode[], LogicNode[]>("/admin/site/logic-nodes", { params: { status } });
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
