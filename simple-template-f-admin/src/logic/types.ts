/**
 * 逻辑层共享类型（管理端与用户端同源副本）。
 *
 * 执行器刻意不依赖 Vue：状态由调用方传入（用户端传 reactive 的对象，headless 测试传普通对象），
 * 这样引擎本身可以在 node 里直接跑，不必为了验证它去起浏览器。
 */

/** 端口声明（入参/出参/设置项通用） */
export interface LogicPort {
    key: string
    type: string
    required?: boolean
    options?: string[]
    note?: string
}

/** 图里的一个节点；typeCode='preset' 时用 presetCode + presetVersion 引用预设 */
export interface LogicNode {
    id: string
    typeCode: string
    label?: string
    config?: Record<string, any>
    presetCode?: string
    presetVersion?: number
}

export interface LogicEdge {
    from: { node: string; port: string }
    to: { node: string; port: string }
}

/** 一段逻辑图 */
export interface LogicGraph {
    nodes: LogicNode[]
    edges: LogicEdge[]
    /** 出参名 -> "<节点id>.<端口>"，仅预设用 */
    exports?: Record<string, string>
}

/** 执行轨迹（调试面板用） */
export interface TraceItem {
    runId: string
    nodeId: string
    typeCode: string
    status: "ok" | "skipped" | "error"
    ms: number
    inputs?: Record<string, any>
    outputs?: Record<string, any>
    message?: string
}

/** 执行器与渲染器共享的运行状态；用户端传 reactive 的以免组件不刷新 */
export interface EngineState {
    /** nodeId -> 端口 -> 值 */
    outputs: Record<string, Record<string, any>>
    /** 组件显隐（act.setVisible 写的，渲染器读的） */
    visible: Record<string, boolean>
    /** 会话（act.login 写入令牌） */
    session: Record<string, any>
    /** 正在奔跑的节点，渲染器可显示加载态 */
    loading: Record<string, boolean>
    /** 执行轨迹，最近一次运行放前面 */
    trace: TraceItem[]
}

/** 宿主能力：引擎只编排，具体怎么跳转/提示/发请求由页面给 */
export interface GraphHost {
    /** 调后端接口；返回接口 data 部分 */
    callApi(method: string, path: string, params: Record<string, any>): Promise<any>
    navigate(url: string, newTab?: boolean): void
    toast(message: string, type?: string): void
    scrollTo(nodeId: number | string): void
    setVisible(nodeId: number | string, visible: boolean): void
    openModal(nodeId?: number | string | null): void
    closeModal(nodeId?: number | string | null): void
    /** 读某个组件当前的输入值（表单类图用） */
    componentValue(nodeId: number | string, field?: string): any
    /** 运行环境：routeParam.id / query.k / page */
    runtimeValues(): Record<string, any>
    isLoggedIn(): boolean
    currentMemberId(): number | null
    logout(): void
    /** 运行期出错的兜底提示（未接 error 出口时） */
    onError?(message: string): void
}

export function emptyState(): EngineState {
    return { outputs: {}, visible: {}, session: {}, loading: {}, trace: [] }
}
