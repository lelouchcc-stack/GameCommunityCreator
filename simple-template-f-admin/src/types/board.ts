/** 与后端 BoardVO 对应 */
export interface Board {
    id: number
    name: string
    intro: string | null
    cover: string | null
    sort: number
    /** 1开放 0关闭 */
    status: number
    currentVersionId: number | null
    currentVersionNo: string | null
    createTime: string
}

/** 新增/编辑提交体，与后端 BoardDTO 对应 */
export interface BoardForm {
    name: string
    intro: string
    cover: string
    sort: number
}

/** 列表查询条件 */
export interface BoardQuery {
    pageNum: number
    pageSize: number
    name?: string
    status?: number
}
