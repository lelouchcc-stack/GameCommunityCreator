/** 与后端 ReportVO 对应 */
export interface ReportItem {
    id: number
    postId: number
    postTitle: string | null
    postContent: string | null
    /** 被举报帖子当前状态：0待审 1已发布 2已下架 */
    postStatus: number | null
    reporterName: string | null
    reason: string | null
    description: string | null
    /** 0待处理 1已处理 2已驳回 */
    status: number
    handleUserName: string | null
    handleRemark: string | null
    handleTime: string | null
    createTime: string
}

export interface ReportQuery {
    pageNum: number
    pageSize: number
    status?: number
}
