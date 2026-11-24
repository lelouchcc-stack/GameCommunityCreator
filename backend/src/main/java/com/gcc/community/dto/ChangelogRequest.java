package com.gcc.community.dto;

import java.time.LocalDate;

/** 新增或修改更新日志的请求参数。 */
public record ChangelogRequest(
        String version,
        String title,
        String bodyMarkdown,
        // 老客户端用的字段，只传它的话也当正文处理
        String content,
        LocalDate releaseDate,
        String status) {
}
