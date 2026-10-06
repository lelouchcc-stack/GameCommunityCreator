import http from "../utils/request";
import type { Tag, TagForm, TagQuery } from "../types/tag";
import type { PageResult } from "../types/common";

export function pageTags(params: TagQuery) {
    return http.get<PageResult<Tag>, PageResult<Tag>>("/admin/tag/page", { params });
}

export function createTag(data: TagForm) {
    return http.post<void, void>("/admin/tag", data);
}

export function updateTag(id: number, data: TagForm) {
    return http.put<void, void>(`/admin/tag/${id}`, data);
}

export function changeTagStatus(id: number, status: number) {
    return http.put<void, void>(`/admin/tag/${id}/status`, null, { params: { status } });
}

export function deleteTag(id: number) {
    return http.delete<void, void>(`/admin/tag/${id}`);
}

/** 用户端内容分类项（与后端 TagVO 对应）：管理端画布预览板块详情的分类区时也走这个接口 */
export interface TagItem {
    id: number
    boardId: number | null
    tagName: string
    /** 1内容类型(攻略/讨论/同人) 2主题 */
    tagType: number
    sort: number
}

/** 启用的内容分类：全局标签 + 指定板块的标签（匿名可读） */
export function listTags(boardId?: number) {
    return http.get<TagItem[], TagItem[]>("/app/tag/list", { params: { boardId } });
}
