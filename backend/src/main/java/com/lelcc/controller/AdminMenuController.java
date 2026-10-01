package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.common.UserContext;
import com.lelcc.dto.MenuVO;
import com.lelcc.service.SysMenuService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/menu")
public class AdminMenuController {

    @Resource
    private SysMenuService sysMenuService;

    /** 当前登录账号可见的菜单树，前端据此生成动态路由与侧边栏 */
    @GetMapping("/tree")
    public Result<List<MenuVO>> tree() {
        return Result.success(sysMenuService.listMenuTree(UserContext.getId()));
    }

    /** 完整菜单树（含按钮与停用项），供角色分配权限时勾选 */
    @GetMapping("/options")
    public Result<List<MenuVO>> options() {
        return Result.success(sysMenuService.listAllMenuTree());
    }
}
