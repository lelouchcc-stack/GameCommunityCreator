package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lelcc.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查某个后台账号已启用的角色。
     * sys_user_role 是纯关联表，没有实体，直接在这里 join。
     */
    @Select("""
            SELECT r.id, r.role_name, r.role_key, r.sort, r.status
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 1
            ORDER BY r.sort
            """)
    List<SysRole> selectEnabledByUserId(@Param("userId") Long userId);
}
