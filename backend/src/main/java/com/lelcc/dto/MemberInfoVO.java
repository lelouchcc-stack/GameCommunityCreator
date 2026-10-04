package com.lelcc.dto;

/** 返回给用户端的玩家信息（不含密码等敏感字段）。 */
public record MemberInfoVO(
        Long id,
        String username,
        String nickname,
        String avatar,
        String intro) {
}
