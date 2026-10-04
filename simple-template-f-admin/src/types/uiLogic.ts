/**
 * 逻辑层的前端类型。
 *
 * 注意命名：**图里的节点**叫 `LogicNode`（id/typeCode/config），**原子库里的一行**叫
 * `LogicAtom`（typeName/kind/runner/端口 schema）。两者都直接复用 `logic/types` 的定义，
 * 避免同一个形状在两处各写一份后走样（这个坑已经踩过一次）。
 */
import type { LogicEdge, LogicGraph, LogicNode, LogicPort } from "../logic/types";

export type { LogicEdge, LogicGraph, LogicNode, LogicPort };

/** 与后端 LogicNodeVO 对应：原子库里的一行（管理端只读） */
export interface LogicAtom {
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

/** 与后端 FlowVO 对应：页面上的一张图 */
export interface FlowVO {
    id: number
    pageId: number
    flowKey: string
    flowName: string | null
    graph: LogicGraph
    status: number
    remark: string | null
    createTime?: string
    updateTime?: string
}

/** 整图保存的请求体（flowKey 走路径参数） */
export interface FlowPayload {
    flowName: string | null
    graphJson: LogicGraph
    status: number
    remark: string | null
}
