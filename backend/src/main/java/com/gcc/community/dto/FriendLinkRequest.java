package com.gcc.community.dto;

/** 新增或修改友情链接的请求参数。 */
public record FriendLinkRequest(
        String name,
        String url,
        String description,
        Integer sortOrder,
        Boolean enabled) {
}
