package com.gcc.community.domain;

import java.time.LocalDateTime;

/**
 * 更新日志。
 */
public record Changelog(
        long id,
        long projectId,
        String version,
        String title,
        String content,        // 旧字段，和 bodyMarkdown 同值
        String bodyMarkdown,   // 正文 Markdown，读的时候优先用它
        java.time.LocalDate releaseDate,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
