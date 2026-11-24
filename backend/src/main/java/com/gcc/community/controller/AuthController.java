package com.gcc.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gcc.community.common.ApiResponse;
import com.gcc.community.dto.AuthTokenView;
import com.gcc.community.dto.LoginRequest;
import com.gcc.community.dto.RegisterRequest;
import com.gcc.community.dto.UserView;
import com.gcc.community.service.AuthService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

/** 注册、登录、退出和当前用户的接口。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    // 注册，成功就直接登录
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthTokenView>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthTokenView view = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(view));
    }

    // 登录
    @PostMapping("/login")
    public ApiResponse<AuthTokenView> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    // 退出登录
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    // 当前登录的用户
    @GetMapping("/me")
    public ApiResponse<UserView> me() {
        return ApiResponse.ok(authService.me());
    }
}
