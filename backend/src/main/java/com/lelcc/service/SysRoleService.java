package com.lelcc.service;

import com.lelcc.common.PageResult;
import com.lelcc.dto.SysRoleDTO;
import com.lelcc.entity.SysRole;

import java.util.List;

public interface SysRoleService {

    PageResult<SysRole> page(String roleName, Integer status, long pageNum, long pageSize);

    /** 启用状态的角色精简列表，供用户管理分配角色时使用 */
    List<SysRole> options();

    void create(SysRoleDTO dto);

    void update(Long id, SysRoleDTO dto);

    /** 该角色已勾选的菜单 ID */
    List<Long> listMenuIds(Long roleId);

    /** 覆盖式重新分配；menuIds 为空表示该角色不拥有任何菜单 */
    void assignMenus(Long roleId, List<Long> menuIds);

    void delete(Long id);
}
