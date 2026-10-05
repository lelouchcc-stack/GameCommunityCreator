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
