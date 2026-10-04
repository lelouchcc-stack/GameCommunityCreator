/** 与后端 NodeVO 对应；children 组成树。
 *  本文件与管理端 simple-template-f-admin/src/types/uiBuilder.ts 保持同源（渲染器依赖）。 */
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
    children: UiNode[]
}
