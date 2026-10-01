package com.lelcc.dto;

/** 后台登录成功的返回结构，对应前端 LoginResult。 */
public record AdminLoginVO(String token, AdminUserInfoVO userInfo) {
}
