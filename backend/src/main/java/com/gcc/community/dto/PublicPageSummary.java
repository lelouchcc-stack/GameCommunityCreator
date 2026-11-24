package com.gcc.community.dto;

/** 公开页面列表里的一条。 */
public record PublicPageSummary(
        String pageId,
        String name,
        String slug,
        String pageType) {
}
