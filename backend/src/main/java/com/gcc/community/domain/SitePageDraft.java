package com.gcc.community.domain;

import java.time.LocalDateTime;

/**
 * 页面草稿，每个页面存一条配置 JSON。
 */
public record SitePageDraft(
        String pageId,
        String configJson,     // 校验过的页面配置 JSON
        int configVersion,     // 乐观锁版本号
        LocalDateTime updatedAt) {
}
