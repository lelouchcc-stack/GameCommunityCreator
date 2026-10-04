import http from "../utils/request";
import type {
    ComponentType,
    ComponentTypePayload,
    NodePayload,
    PagePayload,
    PageVO,
    UiNode,
} from "../types/uiBuilder";

export function listComponentTypes(status?: number) {
    return http.get<ComponentType[], ComponentType[]>("/admin/site/component-types", { params: { status } });
}

export function createComponentType(data: ComponentTypePayload) {
    return http.post<unknown, unknown>("/admin/site/component-types", data);
}

export function updateComponentType(id: number, data: ComponentTypePayload) {
    return http.put<unknown, unknown>(`/admin/site/component-types/${id}`, data);
}

export function deleteComponentType(id: number) {
    return http.delete<unknown, unknown>(`/admin/site/component-types/${id}`);
}

export function listPages() {
    return http.get<PageVO[], PageVO[]>("/admin/site/pages");
}

export function createPage(data: PagePayload) {
    return http.post<unknown, unknown>("/admin/site/pages", data);
}

export function updatePage(id: number, data: PagePayload) {
    return http.put<unknown, unknown>(`/admin/site/pages/${id}`, data);
}

export function deletePage(id: number) {
    return http.delete<unknown, unknown>(`/admin/site/pages/${id}`);
}

export function listNodes(pageId: number) {
    return http.get<UiNode[], UiNode[]>(`/admin/site/pages/${pageId}/nodes`);
}

/** 整树保存：新增节点 id 用负数临时 id，子节点 parentId 指向父节点临时 id */
export function saveNodes(pageId: number, nodes: NodePayload[]) {
    return http.put<UiNode[], UiNode[]>(`/admin/site/pages/${pageId}/nodes`, nodes);
}
