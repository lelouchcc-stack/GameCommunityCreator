package com.gcc.community.domain;

import java.time.LocalDateTime;

/**
 * 站点设置记录，每个站点一条。
 */
public record SiteSettingsRecord(
        long projectId,
        String themeMode,     // DARK / LIGHT / SYSTEM
        LocalDateTime updatedAt) {
}
