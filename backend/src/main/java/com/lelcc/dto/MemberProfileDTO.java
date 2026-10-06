package com.lelcc.dto;

/**
 * 玩家个人资料修改请求体（个人主页用）。
 * 只开放昵称/简介/头像三项：用户名是登录凭据，改它要走独立的重名校验流程。
 */
public record MemberProfileDTO(
        String nickname,
        String intro,
        String avatar) {
}
