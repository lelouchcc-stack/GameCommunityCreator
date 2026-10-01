package com.lelcc.service;

import com.lelcc.dto.MenuVO;

import java.util.List;

public interface SysMenuService {

    /** 按登录账号的角色，返回可见菜单树 */
    List<MenuVO> listMenuTree(Long userId);
}
