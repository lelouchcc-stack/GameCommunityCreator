package com.lelcc.dto;

import jakarta.validation.constraints.NotBlank;

/** 后台登录请求体，字段名与前端 LoginParams 一致。 */
public record AdminLoginDTO(
        @NotBlank(message = "请输入账号") String username,
        @NotBlank(message = "请输入密码") String password) {
}
