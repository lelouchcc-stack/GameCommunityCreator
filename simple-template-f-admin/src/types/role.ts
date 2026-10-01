/** 与后端 SysRole 实体对应 */
export interface SysRole {
    id: number
    roleName: string
    roleKey: string
    sort: number
    /** 1正常 0停用 */
    status: number
    remark: string | null
    createTime: string
}

/** 新增/编辑提交体，与后端 SysRoleDTO 对应 */
export interface SysRoleForm {
    roleName: string
    roleKey: string
    sort: number
    status: number
    remark: string
}

/** 列表查询条件 */
export interface RoleQuery {
    pageNum: number
    pageSize: number
    roleName?: string
    status?: number
}
