package com.lelcc.controller;

import com.lelcc.common.PageResult;
import com.lelcc.common.Result;
import com.lelcc.dto.RoleMenuAssignDTO;
import com.lelcc.dto.SysRoleDTO;
import com.lelcc.entity.SysRole;
import com.lelcc.service.SysRoleService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/role")
public class SysRoleController {

    @Resource
    private SysRoleService sysRoleService;

    @GetMapping("/page")
    public Result<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String roleName,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(sysRoleService.page(roleName, status, pageNum, pageSize));
    }

    /** 精简选项，供用户管理分配角色 */
    @GetMapping("/options")
    public Result<List<SysRole>> options() {
        return Result.success(sysRoleService.options());
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody SysRoleDTO dto) {
        sysRoleService.create(dto);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysRoleDTO dto) {
        sysRoleService.update(id, dto);
        return Result.success(null);
    }

    /** 该角色已勾选的菜单 ID，用于回显权限树 */
    @GetMapping("/{id}/menus")
    public Result<List<Long>> listMenuIds(@PathVariable Long id) {
        return Result.success(sysRoleService.listMenuIds(id));
    }

    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody RoleMenuAssignDTO dto) {
        sysRoleService.assignMenus(id, dto.menuIds());
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleService.delete(id);
        return Result.success(null);
    }
}
