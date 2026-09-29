package com.lelcc.common;

/**
 * 当前请求的登录主体，由拦截器解析令牌后写入 {@link UserContext}。
 */
public record LoginUser(Long id, UserType type) {
}
