package com.lelcc.service;

import com.lelcc.dto.MenuVO;
import com.lelcc.dto.SysMenuDTO;

import java.util.List;

public interface SysMenuService {

    /** 按登录账号的角色，返回可见菜单树 */
    List<MenuVO> listMenuTree(Long userId);

    /** 完整菜单树（含按钮 F 与停用项），供角色分配权限时勾选、以及菜单管理页展示 */
    List<MenuVO> listAllMenuTree();

    void create(SysMenuDTO dto);

    void update(Long id, SysMenuDTO dto);

    /** 有子节点或已被角色引用时会被拒绝 */
    void delete(Long id);
}
