package com.lelcc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 玩家注册请求体。
 * 校验规则按 member.username VARCHAR(50) 与常见社区惯例取值（pit.md 无 01/02 用例表，
 * 见设计文档 decisions.tail[6]）；前后端使用同一组规则。
 */
public record MemberRegisterDTO(
        @NotBlank(message = "请输入用户名")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "用户名需为 4-20 位字母、数字或下划线")
        String username,

        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
        String password,

        @Size(max = 50, message = "昵称不能超过 50 个字符")
        String nickname) {
}
