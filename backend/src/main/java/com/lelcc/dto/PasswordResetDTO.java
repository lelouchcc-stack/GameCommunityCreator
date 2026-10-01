package com.lelcc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 重置密码请求体。 */
public record PasswordResetDTO(
        @NotBlank(message = "请输入新密码")
        @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
        String password) {
}
