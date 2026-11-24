package com.gcc.community.dto;

/** 登录或注册成功后返回的 token 信息。 */
public record AuthTokenView(String token, String tokenType, long expiresIn, UserView user) {
}
