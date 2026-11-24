package com.gcc.community.dto;

import java.time.LocalDateTime;

import com.gcc.community.domain.PageConfig;

/** 公开页面详情。 */
public record PublicPageView(
        String pageId,
        String name,
        String slug,
        String pageType,
        int configVersion,
        LocalDateTime publishedAt,
        PageConfig config) {
}
