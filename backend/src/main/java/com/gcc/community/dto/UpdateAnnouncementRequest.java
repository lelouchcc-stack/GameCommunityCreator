package com.gcc.community.dto;

/** 修改公告的请求参数，字段都可选。 */
public record UpdateAnnouncementRequest(
        String title,
        String bodyMarkdown,
        String content,
        Boolean pinned) {
}
