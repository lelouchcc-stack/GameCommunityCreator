/** 与后端 com.lelcc.dto.MenuVO 一一对应 */
export interface MenuNode {
    id: number
    parentId: number
    menuName: string
    /** M目录 C菜单 F按钮 */
    menuType: string
    path: string | null
    component: string | null
    perms: string | null
    icon: string | null
    sort: number
    /** 1显示 0隐藏 */
    visible: number
    /** 1正常 0停用 */
    status: number
    children: MenuNode[] | null
}

/** 菜单新增/编辑提交体，与后端 SysMenuDTO 对应 */
export interface SysMenuForm {
    parentId: number
    menuName: string
    /** M目录 C菜单 F按钮 */
    menuType: string
    path: string
    component: string
    perms: string
    icon: string
    sort: number
    visible: number
    status: number
}
