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
}
