package com.gcc.community.dto;

/** 管理端修改问题反馈的请求参数，字段都可选。 */
public record IssueUpdateRequest(
        // 状态，只能是 PENDING、PROCESSING 或 RESOLVED
        String status,
        // 处理结果，传空串表示清空
        String resolution,
        // 是否公开，传 true 时状态必须是 RESOLVED
        Boolean isPublic) {
}
