import http from "../utils/request";

/** 统一分页返回体 */
export interface PageData<T> {
    list: T[]
    total: number
}

/** 板块简介（与后端 BoardVO 对应） */
export interface BoardBrief {
    id: number
    name: string
    intro: string | null
    cover: string | null
    sort: number
    status: number
    currentVersionId: number | null
    currentVersionNo: string | null
    createTime: string
}

/** 游戏版本（与后端 BoardVersionVO 对应） */
export interface BoardVersionItem {
    id: number
    boardId: number
    versionNo: string
    versionName: string | null
    releaseTime: string | null
    updateNote: string | null
    status: number
    isCurrent: boolean
    createTime: string
}

/** 进度阶段（与后端 ProgressStageVO 对应） */
export interface ProgressStageItem {
    id: number
    boardId: number
    stageName: string
    description: string | null
    sort: number
    status: number
    createTime: string
}

/** 板块详情（与后端 AppBoardDetailVO 对应） */
export interface BoardDetail {
    board: BoardBrief
    listBoardVersion: BoardVersionItem[]
    listProgressStage: ProgressStageItem[]
}

/** 帖子列表项（与后端 AppPostItemVO 对应） */
export interface PostListItem {
    id: number
    title: string
    cover: string | null
    boardId: number
    boardName: string | null
    versionId: number | null
    versionNo: string | null
    stageId: number | null
    stageName: string | null
    authorName: string | null
    authorAvatar: string | null
    likeCount: number
    viewCount: number
    commentCount: number
    isTop: number
    expired: boolean
    createTime: string
}

/** 帖子详情（与后端 AppPostDetailVO 对应） */
export interface PostDetail {
    id: number
    title: string
    content: string | null
    cover: string | null
    imageUrls: string[]
    boardId: number
    boardName: string | null
    versionId: number | null
    versionNo: string | null
    stageId: number | null
    stageName: string | null
    authorId: number
    authorName: string | null
    authorAvatar: string | null
    likeCount: number
    favoriteCount: number
    commentCount: number
    viewCount: number
    isTop: number
    expired: boolean
    tags: string[]
    createTime: string
}

/** 帖子检索条件 */
export interface PostQuery {
    pageNum?: number
    pageSize?: number
    boardId?: number
    versionId?: number
    stageId?: number
    tagId?: number
    keyword?: string
    sort?: "latest" | "hot"
}

/** 板块详情：匿名可读（后端已对会员拦截器放行 /app/board/*） */
export function getBoardDetail(boardId: number) {
    return http.get<BoardDetail, BoardDetail>(`/app/board/${boardId}`);
}

/** 帖子检索：匿名可读（后端已放行 /app/post/*） */
export function pagePosts(params: PostQuery) {
    return http.get<PageData<PostListItem>, PageData<PostListItem>>("/app/post/page", { params });
}

/** 帖子详情：匿名可读，后端会累加浏览量 */
export function getPost(postId: number) {
    return http.get<PostDetail, PostDetail>(`/app/post/${postId}`);
}
