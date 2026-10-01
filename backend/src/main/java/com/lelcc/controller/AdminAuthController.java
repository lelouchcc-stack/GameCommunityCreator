package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.AdminLoginDTO;
import com.lelcc.dto.AdminLoginVO;
import com.lelcc.service.AdminAuthService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    @Resource
    private AdminAuthService adminAuthService;

    /** 后台登录，/admin/login 已在 WebMvcConfig 中放行 */
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.success(adminAuthService.login(dto));
    }
}
