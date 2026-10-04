package com.lelcc.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 玩家登录请求体。
 * 这里只校验非空——格式规则在注册时把关，登录不该因为历史账号格式不同而被拦下。
 */
public record MemberLoginDTO(
        @NotBlank(message = "请输入用户名") String username,
        @NotBlank(message = "请输入密码") String password) {
}
