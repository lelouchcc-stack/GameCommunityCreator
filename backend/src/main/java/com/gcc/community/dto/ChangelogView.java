package com.gcc.community.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 更新日志的返回结果。 */
public record ChangelogView(
        long id,
        String version,
        String title,
        // 正文，优先读这个
        String bodyMarkdown,
        // 和 bodyMarkdown 一样的值，留给老客户端
        String content,
        LocalDate releaseDate,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
