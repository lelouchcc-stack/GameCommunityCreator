package com.gcc.community.domain;

/**
 * 友情链接，公开端只显示 enabled 的。
 */
public record FriendLink(
        long id,
        long projectId,
        String name,
        String url,
        String description,
        int sortOrder,
        boolean enabled) {
}
