import http from "../utils/request";
import type { Board, BoardForm, BoardQuery } from "../types/board";
import type { PageResult } from "../types/common";

/** 板块分页列表 */
export function pageBoards(params: BoardQuery) {
    return http.get<PageResult<Board>, PageResult<Board>>("/admin/board/page", { params });
}

export function createBoard(data: BoardForm) {
    return http.post<void, void>("/admin/board", data);
}

export function updateBoard(id: number, data: BoardForm) {
    return http.put<void, void>(`/admin/board/${id}`, data);
}

/** 设为开放(1)或关闭(0) */
export function changeBoardStatus(id: number, status: number) {
    return http.put<void, void>(`/admin/board/${id}/status`, null, { params: { status } });
}

/** 板块下有帖子时后端会拒绝并返回原因 */
export function deleteBoard(id: number) {
    return http.delete<void, void>(`/admin/board/${id}`);
}

/** 用户端板块导航用的开放板块项：与后端 BoardVO 对应（管理端预览板导航时也走这个接口） */
export type BoardItem = Board;

/** 开放板块列表：匿名可读（后端已对会员拦截器放行 /app/board/list） */
export function listBoards() {
    return http.get<BoardItem[], BoardItem[]>("/app/board/list");
}
