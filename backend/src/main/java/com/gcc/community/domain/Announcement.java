package com.gcc.community.domain;

import java.time.LocalDateTime;

/**
 * 社区公告。
 */
public record Announcement(
        long id,
        String title,
        String content,        // 旧字段，和 bodyMarkdown 同值
        String bodyMarkdown,   // 正文 Markdown，读的时候优先用它
        boolean pinned,
        LocalDateTime publishedAt) {
}
