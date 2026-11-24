package com.gcc.community.dto;

import java.time.LocalDateTime;

/** 一条发布历史记录。 */
public record PublicationView(
        long publicationId,
        int version,
        LocalDateTime publishedAt) {
}
