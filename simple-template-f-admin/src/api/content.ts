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
    /** 0待审 1已发布 2已下架；公开列表恒为 1，只有「我的帖子」会用到 */
    status?: number
    /** 审核意见/驳回原因，仅「我的帖子」下发 */
    auditRemark?: string | null
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

/** 当前玩家对某帖的互动状态与计数（需登录） */
export interface Interaction {
    liked: boolean
    favorited: boolean
    likeCount: number
    favoriteCount: number
    commentCount: number
}

/** 评论（一级评论带 replies） */
export interface CommentItem {
    id: number
    postId: number
    memberId: number
    authorName: string | null
    authorAvatar: string | null
    parentId: number
    rootId: number
    content: string
    likeCount: number
    createTime: string
    replies: CommentItem[]
}

/** 发表评论/回复请求体 */
export interface CommentForm {
    postId: number
    parentId?: number
    content: string
}

/** 互动状态（需登录；匿名调用会 401） */
export function getInteraction(postId: number) {
    return http.get<Interaction, Interaction>(`/app/post/${postId}/interaction`);
}

/** 点赞/取消点赞（需登录） */
export function likePost(postId: number) {
    return http.post<Interaction, Interaction>(`/app/post/${postId}/like`);
}

/** 收藏/取消收藏（需登录） */
export function favoritePost(postId: number) {
    return http.post<Interaction, Interaction>(`/app/post/${postId}/favorite`);
}

/** 评论分页：匿名可读（后端已放行 /app/comment/page） */
export function pageComments(postId: number, pageNum = 1, pageSize = 10) {
    return http.get<PageData<CommentItem>, PageData<CommentItem>>("/app/comment/page", {
        params: { postId, pageNum, pageSize },
    });
}

/** 发表评论/回复（需登录） */
export function createComment(data: CommentForm) {
    return http.post<CommentItem, CommentItem>("/app/comment", data);
}

/** 发帖请求体（与后端 PostCreateDTO 对应） */
export interface PostCreateForm {
    boardId: number
    versionId: number
    /** 进度阶段，可空表示通用 */
    stageId?: number | null
    title: string
    content: string
    /** 封面：对象存储 objectKey 或完整地址 */
    cover?: string | null
    /** 标签 id，最多 5 个 */
    tagIds?: number[]
}

/** 发帖结果（与后端 PostCreateVO 对应） */
export interface PostCreateResult {
    id: number
    /** 0待审 1已发布 2已下架；新帖恒为 0 */
    status: number
}

/**
 * 发帖（需登录）。
 * 板块与版本都必填：社区按「游戏版本 × 进度阶段」组织内容，帖子不挂版本就没法在正确的版本下被看到。
 * 落库状态是待审核，管理员通过后才出现在公开列表与热门里。
 */
export function createPost(data: PostCreateForm) {
    return http.post<PostCreateResult, PostCreateResult>("/app/post", data);
}
