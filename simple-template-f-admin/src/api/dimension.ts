import http from "../utils/request";
import type {
    BoardVersion,
    BoardVersionForm,
    ProgressStage,
    ProgressStageForm,
    StageSortItem,
} from "../types/dimension";

/* ---------------- 游戏版本 ---------------- */

export function listVersions(boardId: number) {
    return http.get<BoardVersion[], BoardVersion[]>(`/admin/board/${boardId}/versions`);
}

export function createVersion(boardId: number, data: BoardVersionForm) {
    return http.post<void, void>(`/admin/board/${boardId}/versions`, data);
}

export function updateVersion(boardId: number, id: number, data: BoardVersionForm) {
    return http.put<void, void>(`/admin/board/${boardId}/versions/${id}`, data);
}

/** 设为当前版本：后端在同一事务内更新 board.current_version_id */
export function setCurrentVersion(boardId: number, id: number) {
    return http.put<void, void>(`/admin/board/${boardId}/versions/${id}/current`);
}

export function changeVersionStatus(boardId: number, id: number, status: number) {
    return http.put<void, void>(`/admin/board/${boardId}/versions/${id}/status`, null, {
        params: { status },
    });
}

export function deleteVersion(boardId: number, id: number) {
    return http.delete<void, void>(`/admin/board/${boardId}/versions/${id}`);
}

/* ---------------- 进度阶段 ---------------- */

export function listStages(boardId: number) {
    return http.get<ProgressStage[], ProgressStage[]>(`/admin/board/${boardId}/stages`);
}

export function createStage(boardId: number, data: ProgressStageForm) {
    return http.post<void, void>(`/admin/board/${boardId}/stages`, data);
}

export function updateStage(boardId: number, id: number, data: ProgressStageForm) {
    return http.put<void, void>(`/admin/board/${boardId}/stages/${id}`, data);
}

/** 批量调整顺序 */
export function reorderStages(boardId: number, items: StageSortItem[]) {
    return http.put<void, void>(`/admin/board/${boardId}/stages/sort`, items);
}

export function changeStageStatus(boardId: number, id: number, status: number) {
    return http.put<void, void>(`/admin/board/${boardId}/stages/${id}/status`, null, {
        params: { status },
    });
}

export function deleteStage(boardId: number, id: number) {
    return http.delete<void, void>(`/admin/board/${boardId}/stages/${id}`);
}
