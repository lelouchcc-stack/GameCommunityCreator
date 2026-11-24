package com.gcc.community.dto;

import java.time.LocalDateTime;

/** 站点页面的信息。 */
public record SitePageView(
        // 页面 ID，按钮跳转时用它
        String pageId,
        long projectId,
        String name,
        String slug,
        String pageType,
        String templateKey,
        String status,
        // 草稿版本号，修改时用来防冲突
        int draftVersion,
        // 发布过的版本号，没发布过就是 null
        Integer publishedVersion,
        // 是不是首页
        boolean isHome,
        int sortOrder,
        LocalDateTime updatedAt) {
}
