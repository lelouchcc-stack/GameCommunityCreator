package com.gcc.community.domain;

import java.time.LocalDateTime;

/**
 * 社区项目。
 */
public record CommunityProject(
        long id,
        String name,
        String slug,
        String gameName,
        String description,
        String status,
        Long ownerUserId,       // 所属账号 id，旧数据可能是 null
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
