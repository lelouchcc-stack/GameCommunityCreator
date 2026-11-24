package com.gcc.community.domain;

import java.time.LocalDateTime;

/**
 * 问题反馈，公开端只能提交。
 */
public record IssueReport(
        long id,
        long projectId,
        String title,
        String category,
        String description,
        String gameVersion,
        String steps,
        String status,          // PENDING / PROCESSING / RESOLVED
        String resolution,      // 处理结果，管理员填
        boolean isPublic,       // 允许公开展示
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
