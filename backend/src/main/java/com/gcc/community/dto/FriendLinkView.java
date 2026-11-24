package com.gcc.community.dto;

/** 友情链接的信息。 */
public record FriendLinkView(
        long id,
        String name,
        String url,
        String description,
        int sortOrder,
        boolean enabled) {
}
