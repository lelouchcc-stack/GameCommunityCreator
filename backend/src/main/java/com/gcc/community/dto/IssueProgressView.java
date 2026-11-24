package com.gcc.community.dto;

import java.time.LocalDateTime;

/** 公开的问题处理进展。 */
public record IssueProgressView(
        long id,
        String title,
        String category,
        String description,
        String resolution,
        LocalDateTime updatedAt) {
}
