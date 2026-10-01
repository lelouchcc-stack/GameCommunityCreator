/** 与后端 com.lelcc.common.PageResult 对应 */
export interface PageResult<T> {
    list: T[]
    total: number
}
