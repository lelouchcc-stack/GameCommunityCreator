import http from "../utils/request";

/** 趋势图的一天 */
export interface StatTrendPoint {
    date: string
    posts: number
    comments: number
}

export interface StatBoardPoint {
    boardId: number
    boardName: string
    posts: number
}

export interface StatVersionPoint {
    versionNo: string
    posts: number
    current: boolean
}

/** 与后端 StatOverviewVO 对应 */
export interface StatOverview {
    days: number
    totalMembers: number
    totalPosts: number
    pendingPosts: number
    totalComments: number
    totalBoards: number
    totalNotices: number
    pendingReports: number
    rangeNewMembers: number
    rangeNewPosts: number
    rangeNewComments: number
    trend: StatTrendPoint[]
    topBoards: StatBoardPoint[]
    versions: StatVersionPoint[]
}

/** 总览统计：累计快照 + 近 days 天新增与趋势 */
export function getStatOverview(days: number) {
    return http.get<StatOverview, StatOverview>("/admin/stat/overview", { params: { days } });
}
