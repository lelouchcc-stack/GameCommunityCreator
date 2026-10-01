import http from "../utils/request";
import type { MenuNode, SysMenuForm } from "../types/menu";

/** 当前账号可见的菜单树 */
export function getMenuTree() {
    return http.get<MenuNode[], MenuNode[]>("/admin/menu/tree");
}

/** 完整菜单树（含按钮与停用项），供角色分配权限时勾选、以及菜单管理页展示 */
export function getMenuOptions() {
    return http.get<MenuNode[], MenuNode[]>("/admin/menu/options");
}

export function createMenu(data: SysMenuForm) {
    return http.post<void, void>("/admin/menu", data);
}

export function updateMenu(id: number, data: SysMenuForm) {
    return http.put<void, void>(`/admin/menu/${id}`, data);
}

/** 有子节点、或已被角色引用时，后端会拒绝并说明原因 */
export function deleteMenu(id: number) {
    return http.delete<void, void>(`/admin/menu/${id}`);
}
