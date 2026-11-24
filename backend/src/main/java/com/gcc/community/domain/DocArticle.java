package com.gcc.community.domain;

import java.time.LocalDateTime;

/**
 * 公开文档。
 */
public record DocArticle(
        long id,
        long projectId,
        String title,
        String category,
        String content,        // 旧字段，和 bodyMarkdown 同值
        String bodyMarkdown,   // 正文 Markdown，读的时候优先用它
        String slug,           // 站内唯一
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
