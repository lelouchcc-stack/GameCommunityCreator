package com.lelcc.common;

/**
 * 登录身份域。
 * 后台账号(sys_user)与玩家(member)是两套独立的账号体系，
 * 令牌中携带该标记，使一个令牌无法跨端使用。
 */
public enum UserType {
    ADMIN,
    MEMBER
}
