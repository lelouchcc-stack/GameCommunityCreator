import http from "../utils/request";
import type { PostAuditItem, PostAuditQuery } from "../types/post-audit";
import type { PageResult } from "../types/common";

/** 帖子审核列表 */
export function pagePosts(params: PostAuditQuery) {
    return http.get<PageResult<PostAuditItem>, PageResult<PostAuditItem>>("/admin/post/page", { params });
}

/** 审核帖子：通过(1)或下架(2)，下架时必须给原因 */
export function auditPost(id: number, status: number, reason: string) {
    return http.put<void, void>(`/admin/post/${id}/audit`, { status, reason });
}
