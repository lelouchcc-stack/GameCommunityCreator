import http from "../utils/request";
import type { MenuNode } from "../types/menu";

/** 当前账号可见的菜单树 */
export function getMenuTree() {
    return http.get<MenuNode[], MenuNode[]>("/admin/menu/tree");
}

/** 完整菜单树（含按钮与停用项），供角色分配权限时勾选 */
export function getMenuOptions() {
    return http.get<MenuNode[], MenuNode[]>("/admin/menu/options");
}
