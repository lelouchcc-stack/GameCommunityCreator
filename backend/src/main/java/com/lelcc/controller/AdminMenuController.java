package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.Result;
import com.lelcc.common.UserContext;
import com.lelcc.dto.MenuVO;
import com.lelcc.dto.SysMenuDTO;
import com.lelcc.service.SysMenuService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** 完整菜单树（含按钮与停用项），供角色分配权限时勾选、以及菜单管理页展示 */
    @GetMapping("/options")
    public Result<List<MenuVO>> options() {
        return Result.success(sysMenuService.listAllMenuTree());
    }

    @OperLog(module = "菜单管理", operation = "新增菜单")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody SysMenuDTO dto) {
        sysMenuService.create(dto);
        return Result.success(null);
    }

    @OperLog(module = "菜单管理", operation = "修改菜单")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysMenuDTO dto) {
        sysMenuService.update(id, dto);
        return Result.success(null);
    }

    @OperLog(module = "菜单管理", operation = "删除菜单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysMenuService.delete(id);
        return Result.success(null);
    }
}
