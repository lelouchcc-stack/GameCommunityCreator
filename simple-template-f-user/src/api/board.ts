import http from "../utils/request";

/** 与后端 BoardVO 对应（板块导航只需要这几个字段） */
export interface BoardItem {
    id: number
    name: string
    intro: string | null
    cover: string | null
    sort: number
    status: number
    currentVersionId: number | null
    currentVersionNo: string | null
}

/** 开放板块列表：匿名可读（后端已对会员拦截器放行 /app/board/list） */
export function listBoards() {
    return http.get<BoardItem[], BoardItem[]>("/app/board/list");
}
