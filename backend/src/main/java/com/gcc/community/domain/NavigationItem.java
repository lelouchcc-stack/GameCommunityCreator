package com.gcc.community.domain;

/**
 * 站点导航条目。
 */
public record NavigationItem(
        long id,
        long projectId,
        String location,          // header 或 footer
        String title,
        String targetType,        // page 或 external
        String targetPageId,      // targetType=page 时用
        String url,               // targetType=external 时用
        int sortOrder,
        boolean visible) {
}
