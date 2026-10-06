package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lelcc.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查某个后台账号可见的目录与菜单（不含按钮 F），已按父级和序号排好序。
     * 同一个人通过多个角色拿到同一条菜单时会重复，用 DISTINCT 去重。
     */
    @Select("""
            SELECT DISTINCT m.id, m.parent_id, m.menu_name, m.menu_type,
                            m.path, m.component, m.perms, m.icon, m.sort
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId}
              AND m.menu_type IN ('M', 'C')
              AND m.status = 1
              AND m.visible = 1
            ORDER BY m.parent_id, m.sort
            """)
    List<SysMenu> selectVisibleMenusByUserId(@Param("userId") Long userId);

    /**
     * 全部菜单，含按钮(F)与停用项，且不按角色过滤——供"为角色分配权限"时勾选。
     */
    @Select("""
            SELECT id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status
            FROM sys_menu
            ORDER BY parent_id, sort
            """)
    List<SysMenu> selectAll();

    /**
     * 某个后台账号实际拥有的权限标识（多角色取并集）。
     * 角色与菜单任一被停用都不计入，这样停用角色/菜单即刻收回权限。
     */
    @Select("""
            SELECT DISTINCT m.perms
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND m.perms IS NOT NULL AND m.perms <> ''
              AND m.status = 1
              AND r.status = 1
            """)
    List<String> selectPermsByUserId(@Param("userId") Long userId);
}
