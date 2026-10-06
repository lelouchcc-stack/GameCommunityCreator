/** 与后端 NoticeVO 对应：公告（后台分页会带出 sort/status/updateTime） */
export interface Notice {
    id: number
    title: string
    content: string | null
    /** 1公告 2版本更新说明 */
    type: number
    sort?: number
    /** 1发布 0草稿 */
    status?: number
    createTime: string
    updateTime?: string
}

/** 公告新增/编辑请求体 */
export interface NoticeForm {
    title: string
    content: string
    type: number
    sort: number
    status: number
}

/** 公告分页查询条件 */
export interface NoticeQuery {
    pageNum: number
    pageSize: number
    status?: number
    type?: number
    keyword?: string
}
