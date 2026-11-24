package com.gcc.community.dto;

import java.time.LocalDateTime;

/** 问题反馈的详情。 */
public record IssueReportView(
        long id,
        String title,
        String category,
        String description,
        String gameVersion,
        String steps,
        String status,
        // 处理结果，管理员填的
        String resolution,
        // 是不是公开，只有处理完的才能公开
        boolean isPublic,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
