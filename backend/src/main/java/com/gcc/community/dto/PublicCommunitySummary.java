package com.gcc.community.dto;

import java.time.LocalDateTime;

/** 公开社区列表里的一条。 */
public record PublicCommunitySummary(
        long id,
        String name,
        String slug,
        String gameName,
        String description,
        String coverUrl,
        int publishedVersion,
        LocalDateTime publishedAt) {
}
