import http from "../utils/request";
import type { PageResult } from "../types/common";
import type { Notice, NoticeForm, NoticeQuery } from "../types/notice";

/* ---------------- 后台公告管理 ---------------- */

export function pageNoticesAdmin(params: NoticeQuery) {
    return http.get<PageResult<Notice>, PageResult<Notice>>("/admin/notice/page", { params });
}

/** 后台详情：草稿也能读，编辑弹窗回填用 */
export function getNoticeAdmin(id: number) {
    return http.get<Notice, Notice>(`/admin/notice/${id}`);
}

export function createNotice(data: NoticeForm) {
    return http.post<void, void>("/admin/notice", data);
}

export function updateNotice(id: number, data: NoticeForm) {
    return http.put<void, void>(`/admin/notice/${id}`, data);
}

/** 发布(1) / 转草稿(0) */
export function changeNoticeStatus(id: number, status: number) {
    return http.put<void, void>(`/admin/notice/${id}/status`, null, { params: { status } });
}

export function deleteNotice(id: number) {
    return http.delete<void, void>(`/admin/notice/${id}`);
}

/* ---------------- 用户端只读（画布预览也用） ---------------- */

/** 与后端 NoticeVO 对应 */
export interface NoticeItem {
    id: number
    title: string
    content: string | null
    /** 1公告 2版本更新说明 */
    type: number
    createTime: string
}

/** 已发布公告：匿名可读（管理端画布预览公告栏时也走这个接口） */
export function listNotices(limit = 5) {
    return http.get<NoticeItem[], NoticeItem[]>("/app/notice/list", { params: { limit } });
}

/** 统一分页返回体（公告列表页） */
export interface NoticePage {
    list: NoticeItem[]
    total: number
}

/** 公告列表分页：管理端画布预览 builtin:notice_list 时也走这个接口 */
export function pageNotices(pageNum = 1, pageSize = 10) {
    return http.get<NoticePage, NoticePage>("/app/notice/page", { params: { pageNum, pageSize } });
}

/** 公告详情：管理端画布预览 builtin:notice_detail 时也走这个接口 */
export function getNotice(id: number) {
    return http.get<NoticeItem, NoticeItem>(`/app/notice/${id}`);
}
