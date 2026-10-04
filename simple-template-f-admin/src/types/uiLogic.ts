/** 端口：入参 / 出参 / 节点设置项都用它描述（与后端 inputs_schema 等 JSON 结构对应） */
export interface LogicPort {
    key: string
    type: string
    required?: boolean
    options?: string[]
    note?: string
}

/** 一段逻辑图：nodes + edges，预设另有 exports */
export interface LogicGraph {
    nodes: Array<Record<string, any>>
    edges: Array<Record<string, any>>
    exports?: Record<string, string>
}

/** 与后端 LogicNodeVO 对应：原子库里的一个原子（管理端只读） */
export interface LogicNode {
    id: number
    typeCode: string
    typeName: string
    /** trigger/source/operator/condition/action/ui */
    kind: string
    category: string | null
    /** api=调后端接口 / ui=前端本地动作 */
    runner: string
    sourceApi: string | null
    inputs: LogicPort[]
    outputs: LogicPort[]
    config: LogicPort[]
    previewIcon: string | null
    sort: number
    status: number
    remark: string | null
}

/** 与后端 PresetVO 对应：一个原子团（预设） */
export interface Preset {
    id: number
    presetCode: string
    presetName: string
    version: number
    /** 1内置（只读，可复制为自定义） */
    isBuiltin: number
    category: string | null
    inputs: LogicPort[]
    outputs: LogicPort[]
    graph: LogicGraph
    status: number
    remark: string | null
    createTime?: string
    updateTime?: string
}

/** 新增/编辑预设的请求体 */
export interface PresetPayload {
    presetName: string
    category: string | null
    inputsSchema: LogicPort[]
    outputsSchema: LogicPort[]
    graphJson: LogicGraph
    status: number
    remark: string | null
}
