package com.gcc.community.dto;

import jakarta.validation.constraints.Size;

/** 注册的请求参数。 */
public record RegisterRequest(
        String username,

        @Size(max = 72, message = "密码长度不能超过 72 个字符")
        String password,

        String displayName) {
}
