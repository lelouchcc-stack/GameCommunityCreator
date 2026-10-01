import http from "../utils/request";
import type { MenuNode } from "../types/menu";

/** 当前账号可见的菜单树 */
export function getMenuTree() {
    return http.get<MenuNode[], MenuNode[]>("/admin/menu/tree");
}
