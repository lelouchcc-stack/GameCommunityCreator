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

/** 已发布公告：匿名可读（管理端画布预览公告栏时也走这个接口） */
export function listNotices(limit = 5) {
    return http.get<NoticeItem[], NoticeItem[]>("/app/notice/list", { params: { limit } });
}
