/** 与后端 BoardVersionVO 对应 */
export interface BoardVersion {
    id: number
    boardId: number
    versionNo: string
    versionName: string | null
    releaseTime: string | null
    updateNote: string | null
    /** 1正常 0停用 */
    status: number
    /** 是否该板块的当前版本 */
    isCurrent: boolean
    createTime: string
}

/** 新增/编辑提交体，与后端 BoardVersionDTO 对应 */
export interface BoardVersionForm {
    versionNo: string
    versionName: string
    releaseTime: string
    updateNote: string
}

/** 与后端 ProgressStageVO 对应 */
export interface ProgressStage {
    id: number
    boardId: number
    stageName: string
    description: string | null
    sort: number
    /** 1正常 0停用 */
    status: number
    createTime: string
}

/** 新增/编辑提交体，与后端 ProgressStageDTO 对应 */
export interface ProgressStageForm {
    stageName: string
    description: string
    sort: number
}

/** 阶段排序项，与后端 StageSortDTO 对应 */
export interface StageSortItem {
    id: number
    sort: number
}
