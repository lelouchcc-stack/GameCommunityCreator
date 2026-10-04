import http from "../utils/request";

/** 与后端 HotPostVO 对应（板块名与作者昵称已由后端左联接带出） */
export interface HotPostItem {
    id: number
    title: string
    cover: string | null
    boardId: number
    boardName: string | null
    authorName: string | null
    authorAvatar: string | null
    likeCount: number
    viewCount: number
    commentCount: number
    /** 1置顶 0普通 */
    isTop: number
    createTime: string
}

/** 热门帖子：匿名可读（管理端画布预览热门帖子时也走这个接口） */
export function listHotPosts(limit = 10) {
    return http.get<HotPostItem[], HotPostItem[]>("/app/post/hot", { params: { limit } });
}
