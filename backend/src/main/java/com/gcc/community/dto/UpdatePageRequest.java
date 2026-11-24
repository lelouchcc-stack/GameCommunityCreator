package com.gcc.community.dto;

/** 更新页面信息的请求参数，字段都可选。 */
public record UpdatePageRequest(
        String name,
        String slug,
        // 页面状态，只能是 DRAFT、PUBLISHED 或 DISABLED
        String status,
        String templateKey) {
}
