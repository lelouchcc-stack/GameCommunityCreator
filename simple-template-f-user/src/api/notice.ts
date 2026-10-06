import http from "../utils/request";

/** 与后端 NoticeVO 对应 */
export interface NoticeItem {
    id: number
    title: string
    content: string | null
    /** 1公告 2版本更新说明 */
    type: number
    createTime: string
}

/** 统一分页返回体 */
export interface NoticePage {
    list: NoticeItem[]
    total: number
}

/** 已发布公告：匿名可读（后端已放行 /app/notice/*） */
export function listNotices(limit = 5) {
    return http.get<NoticeItem[], NoticeItem[]>("/app/notice/list", { params: { limit } });
}

/** 公告列表页：已发布公告分页，匿名可读 */
export function pageNotices(pageNum = 1, pageSize = 10) {
    return http.get<NoticePage, NoticePage>("/app/notice/page", { params: { pageNum, pageSize } });
}

/** 公告详情：草稿对外不可见，匿名可读 */
export function getNotice(id: number) {
    return http.get<NoticeItem, NoticeItem>(`/app/notice/${id}`);
}
