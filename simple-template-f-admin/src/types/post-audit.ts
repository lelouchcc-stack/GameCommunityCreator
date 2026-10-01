/** 与后端 PostAuditVO 对应 */
export interface PostAuditItem {
    id: number
    title: string
    content: string | null
    boardName: string | null
    authorName: string | null
    /** 关联版本号，通用帖为 null */
    versionNo: string | null
    /** 关联进度阶段名，通用帖为 null */
    stageName: string | null
    /** 0待审 1已发布 2已下架 */
    status: number
    /** 审核意见 / 下架原因 */
    auditRemark: string | null
    createTime: string
}

export interface PostAuditQuery {
    pageNum: number
    pageSize: number
    status?: number
    keyword?: string
}
