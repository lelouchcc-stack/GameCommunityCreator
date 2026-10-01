import http from "../utils/request";
import type { RoleQuery, SysRole, SysRoleForm } from "../types/role";
import type { PageResult } from "../types/common";

/** 角色分页列表 */
export function pageRoles(params: RoleQuery) {
    return http.get<PageResult<SysRole>, PageResult<SysRole>>("/admin/role/page", { params });
}

/** 启用状态的角色选项，供用户管理分配角色 */
export function roleOptions() {
    return http.get<SysRole[], SysRole[]>("/admin/role/options");
}

export function createRole(data: SysRoleForm) {
    return http.post<void, void>("/admin/role", data);
}

export function updateRole(id: number, data: SysRoleForm) {
    return http.put<void, void>(`/admin/role/${id}`, data);
}

/** 内置超管角色、已被账号引用的角色，后端会拒绝删除并说明原因 */
export function deleteRole(id: number) {
    return http.delete<void, void>(`/admin/role/${id}`);
}

/** 该角色已勾选的菜单 ID，用于权限树回显 */
export function getRoleMenuIds(id: number) {
    return http.get<number[], number[]>(`/admin/role/${id}/menus`);
}

/** 覆盖式重新分配权限；传空数组表示不分配任何菜单 */
export function assignRoleMenus(id: number, menuIds: number[]) {
    return http.put<void, void>(`/admin/role/${id}/menus`, { menuIds });
}
