package com.gcc.community.dto;

/** 发布一个页面的结果。 */
public record PublishPageResult(
        String pageId,
        String pageSlug,
        long publicationId,
        int version,
        java.time.LocalDateTime publishedAt,
        String publicPath) {
}
