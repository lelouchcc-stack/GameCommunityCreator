/** 与后端 SysOperLog 实体对应 */
export interface SysOperLog {
    id: number
    userId: number | null
    username: string | null
    /** 业务模块，如"板块管理" */
    module: string | null
    /** 操作描述，如"新增板块" */
    operation: string | null
    method: string | null
    /** 请求参数 JSON，敏感字段已脱敏 */
    params: string | null
    ip: string | null
    /** 1成功 0失败 */
    status: number
    errorMsg: string | null
    /** 耗时（毫秒） */
    costTime: number | null
    createTime: string
}

/** 列表查询条件 */
export interface OperLogQuery {
    pageNum: number
    pageSize: number
    username?: string
    module?: string
    beginTime?: string
    endTime?: string
}
