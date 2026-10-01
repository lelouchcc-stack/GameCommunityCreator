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
    children: MenuNode[] | null
}
