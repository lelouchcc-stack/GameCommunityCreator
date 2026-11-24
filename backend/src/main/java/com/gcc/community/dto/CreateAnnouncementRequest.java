package com.gcc.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 发布公告的请求参数。 */
public record CreateAnnouncementRequest(
        @NotBlank(message = "公告标题不能为空")
        @Size(max = 120, message = "公告标题长度不能超过 120 个字符")
        String title,

        @Size(max = 10000, message = "公告正文长度不能超过 10000 个字符")
        String bodyMarkdown,

        // 老客户端用的字段，只传它的话也当正文处理
        @Size(max = 10000, message = "公告正文长度不能超过 10000 个字符")
        String content,

        Boolean pinned) {
}
