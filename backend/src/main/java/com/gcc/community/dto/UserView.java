package com.gcc.community.dto;

/** 当前登录用户的信息。 */
public record UserView(long id, String username, String displayName) {
}
