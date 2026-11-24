package com.gcc.community.dto;

import java.time.LocalDateTime;

/** 文档的返回结果。 */
public record DocView(
        long id,
        String title,
        String category,
        // 正文，优先读这个
        String bodyMarkdown,
        // 和 bodyMarkdown 一样的值，留给老客户端
        String content,
        String slug,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
