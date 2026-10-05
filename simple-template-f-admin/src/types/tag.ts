/** 与后端 TagVO 对应 */
export interface Tag {
    id: number
    boardId: number | null
    boardName: string | null
    tagName: string
    /** 1内容类型 2主题 */
    tagType: number
    sort: number
    /** 1正常 0停用 */
    status: number
    createTime: string
}

/** 新增/编辑提交体，与后端 TagDTO 对应（boardId 为空=全局标签） */
export interface TagForm {
    boardId: number | null
    tagName: string
    tagType: number
    sort: number
}

/** 列表查询条件 */
export interface TagQuery {
    pageNum: number
    pageSize: number
    boardId?: number
    tagType?: number
    status?: number
    keyword?: string
}
