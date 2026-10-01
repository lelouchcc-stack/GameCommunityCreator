import http from "../utils/request";
import type { OperLogQuery, SysOperLog } from "../types/oper-log";
import type { PageResult } from "../types/common";

/** 操作日志分页列表 */
export function pageOperLogs(params: OperLogQuery) {
    return http.get<PageResult<SysOperLog>, PageResult<SysOperLog>>("/admin/log/page", { params });
}

/** 批量删除历史日志 */
export function deleteOperLogs(ids: number[]) {
    return http.delete<void, void>(`/admin/log/${ids.join(",")}`);
}
