package com.lelcc.dto;

import java.util.List;

/** 返回给前端的登录用户信息，对应前端 UserInfo。 */
public record AdminUserInfoVO(
        Long id,
        String username,
        String nickname,
        String avatar,
        /** 角色标识列表，如 ["admin"] */
        List<String> roles) {
}
