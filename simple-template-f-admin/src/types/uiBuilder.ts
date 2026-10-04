/** 与后端 ComponentTypeVO 对应 */
export interface ComponentType {
    id: number
    typeCode: string
    typeName: string
    category: string
    renderKind: string
    isContainer: number
    isBuiltin: number
    styleSchema: string[]
    contentSchema: string[]
    behaviorSchema: string[]
    defaultStyle: Record<string, any>
    defaultContent: Record<string, any>
    previewIcon: string | null
    sort: number
    status: number
    remark: string | null
    createTime: string
}

export interface ComponentTypePayload {
    typeName: string
    category: string
    renderKind: string
    isContainer: number
    styleSchema: string[]
    contentSchema: string[]
    behaviorSchema: string[]
    defaultStyle: Record<string, any>
    defaultContent: Record<string, any>
    previewIcon?: string | null
    sort?: number
    remark?: string | null
}

/** 与后端 PageVO 对应 */
export interface PageVO {
    id: number
    pageKey: string
    pageName: string
    routePath: string | null
    isHome: number
    status: number
    remark: string | null
    nodeCount: number
    createTime: string
    updateTime: string
}

export interface PagePayload {
    pageKey: string
    pageName: string
    routePath?: string | null
    isHome?: number
    status?: number
    remark?: string | null
}

/** 与后端 NodeVO 对应；children 组成树 */
export interface UiNode {
    id: number
    parentId: number | null
    typeCode: string
    typeName: string | null
    renderKind: string | null
    isContainer: number
    isBuiltin: number
    nodeName: string | null
    sort: number
    visible: number
    style: Record<string, any>
    content: Record<string, any>
    behavior: Record<string, any>
    remark: string | null
    /** 整树保存后回传的"临时 id"（负数）：仅本次新增的节点有值，用于回填动作目标 */
    clientId?: number | null
    children: UiNode[]
}

/** 整树保存的节点项；新增节点 id 用负数临时 id */
export interface NodePayload {
    id: number | null
    parentId: number | null
    typeCode: string
    nodeName: string | null
    sort: number
    visible: number
    styleJson: Record<string, any>
    contentJson: Record<string, any>
    behaviorJson: Record<string, any>
    remark?: string | null
}
