/** 与后端 SysUserVO 对应 */
export interface SysUser {
    id: number
    username: string
    nickname: string | null
    phone: string | null
    email: string | null
    /** 1正常 0停用 */
    status: number
    remark: string | null
    /** 逗号拼接的角色名，仅用于列表展示 */
    roleNames: string | null
    createTime: string
}

/** 新增/编辑提交体，与后端 SysUserDTO 对应 */
export interface SysUserForm {
    username: string
    nickname: string
    phone: string
    email: string
    status: number
    remark: string
    /** 仅新增时提交，编辑走重置密码接口 */
    password: string
    roleIds: number[]
}

/** 列表查询条件 */
export interface SysUserQuery {
    pageNum: number
    pageSize: number
    username?: string
    phone?: string
    status?: number
}
