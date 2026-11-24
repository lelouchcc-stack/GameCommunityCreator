package com.gcc.community.dto;

/** 新增或修改文档的请求参数。 */
public record DocRequest(
        String title,
        String category,
        String bodyMarkdown,
        // 老客户端用的字段，只传它的话也当正文处理
        String content,
        String slug,
        String status) {
}
