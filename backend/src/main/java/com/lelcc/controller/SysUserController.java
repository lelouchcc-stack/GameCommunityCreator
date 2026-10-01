package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.PageResult;
import com.lelcc.common.Result;
import com.lelcc.dto.PasswordResetDTO;
import com.lelcc.dto.SysUserDTO;
import com.lelcc.dto.SysUserVO;
import com.lelcc.service.SysUserService;
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
@RequestMapping("/admin/user")
public class SysUserController {

    @Resource
    private SysUserService sysUserService;

    @GetMapping("/page")
    public Result<PageResult<SysUserVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String username,
                                              @RequestParam(required = false) String phone,
                                              @RequestParam(required = false) Integer status) {
        return Result.success(sysUserService.page(username, phone, status, pageNum, pageSize));
    }

    /** 该账号已分配的角色 ID，用于编辑表单回显 */
    @GetMapping("/{id}/roles")
    public Result<List<Long>> listRoleIds(@PathVariable Long id) {
        return Result.success(sysUserService.listRoleIds(id));
    }

    @OperLog(module = "用户管理", operation = "新增账号")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody SysUserDTO dto) {
        sysUserService.create(dto);
        return Result.success(null);
    }

    @OperLog(module = "用户管理", operation = "修改账号")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysUserDTO dto) {
        sysUserService.update(id, dto);
        return Result.success(null);
    }

    @OperLog(module = "用户管理", operation = "修改账号状态")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        sysUserService.changeStatus(id, status);
        return Result.success(null);
    }

    @OperLog(module = "用户管理", operation = "重置账号密码")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetDTO dto) {
        sysUserService.resetPassword(id, dto.password());
        return Result.success(null);
    }

    @OperLog(module = "用户管理", operation = "删除账号")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.success(null);
    }
}
