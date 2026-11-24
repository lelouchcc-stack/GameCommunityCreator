package com.gcc.community.dto;

/** 导航整体替换时传进来的一项。 */
public record NavigationItemInput(
        String location,
        String title,
        String targetType,
        String targetPageId,
        String url,
        Integer sortOrder,
        Boolean visible) {
}
