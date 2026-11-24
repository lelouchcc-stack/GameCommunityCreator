package com.gcc.community.domain;

import java.time.LocalDateTime;

/**
 * 站点页面，一个项目可以有多个页面。
 */
public record SitePage(
        String id,
        long projectId,
        String name,
        String slug,
        String pageType,
        String templateKey,
        String status,              // DRAFT / PUBLISHED / DISABLED
        int sortOrder,
        int draftVersion,           // 乐观锁版本号
        Integer publishedVersion,   // 没发布过就是 null
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
