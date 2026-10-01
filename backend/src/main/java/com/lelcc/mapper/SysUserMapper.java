package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lelcc.dto.SysUserVO;
import com.lelcc.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 后台账号分页列表，顺带把所属角色名拼出来。
     * 用相关子查询而不是 join + group by，避免分页时 COUNT 与结果集不一致的坑。
     */
    @Select("""
            <script>
            SELECT u.id, u.username, u.nickname, u.phone, u.email, u.status, u.remark, u.create_time,
                   (SELECT GROUP_CONCAT(r.role_name ORDER BY r.sort SEPARATOR ', ')
                      FROM sys_user_role ur
                      JOIN sys_role r ON r.id = ur.role_id
                     WHERE ur.user_id = u.id) AS role_names
            FROM sys_user u
            <where>
                <if test="username != null and username != ''">
                    AND u.username LIKE CONCAT('%', #{username}, '%')
                </if>
                <if test="phone != null and phone != ''">
                    AND u.phone LIKE CONCAT('%', #{phone}, '%')
                </if>
                <if test="status != null">
                    AND u.status = #{status}
                </if>
            </where>
            ORDER BY u.id
            </script>
            """)
    IPage<SysUserVO> selectUserPage(IPage<SysUserVO> page,
                                    @Param("username") String username,
                                    @Param("phone") String phone,
                                    @Param("status") Integer status);
}
