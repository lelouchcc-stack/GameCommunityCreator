package com.lelcc.dto;

/**
 * 玩家登录/注册成功的返回结构。
 * 注册即登录，所以注册接口复用同一结构、一同返回令牌。
 */
public record MemberLoginVO(String token, MemberInfoVO member) {
}
