import http from "../utils/request";
import type { ReportItem, ReportQuery } from "../types/report";
import type { PageResult } from "../types/common";

/** 举报分页列表 */
export function pageReports(params: ReportQuery) {
    return http.get<PageResult<ReportItem>, PageResult<ReportItem>>("/admin/report/page", { params });
}

/**
 * 处理或驳回举报。
 * offlinePost 为 true 时会同时下架被举报的帖子，此时 remark 必填。
 */
export function handleReport(id: number, status: number, remark: string, offlinePost: boolean) {
    return http.put<void, void>(`/admin/report/${id}/handle`, { status, remark, offlinePost });
}
