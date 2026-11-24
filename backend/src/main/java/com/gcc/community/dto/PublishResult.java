package com.gcc.community.dto;

import java.time.LocalDateTime;

/** 发布的结果。 */
public record PublishResult(
        long publicationId,
        int version,
        LocalDateTime publishedAt,
        String publicPath) {
}
