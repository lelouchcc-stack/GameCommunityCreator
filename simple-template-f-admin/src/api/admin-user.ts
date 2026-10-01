import http from "../utils/request";
import type { SysUser, SysUserForm, SysUserQuery } from "../types/admin-user";
import type { PageResult } from "../types/common";

/** 后台账号分页列表 */
export function pageUsers(params: SysUserQuery) {
    return http.get<PageResult<SysUser>, PageResult<SysUser>>("/admin/user/page", { params });
}

/** 该账号已分配的角色 ID，用于编辑表单回显 */
export function getUserRoleIds(id: number) {
    return http.get<number[], number[]>(`/admin/user/${id}/roles`);
}

export function createUser(data: SysUserForm) {
    return http.post<void, void>("/admin/user", data);
}

export function updateUser(id: number, data: SysUserForm) {
    return http.put<void, void>(`/admin/user/${id}`, data);
}

/** 启用(1)或停用(0) */
export function changeUserStatus(id: number, status: number) {
    return http.put<void, void>(`/admin/user/${id}/status`, null, { params: { status } });
}

export function resetUserPassword(id: number, password: string) {
    return http.put<void, void>(`/admin/user/${id}/password`, { password });
}

/** 内置超管账号，后端会拒绝删除并说明原因 */
export function deleteUser(id: number) {
    return http.delete<void, void>(`/admin/user/${id}`);
}
