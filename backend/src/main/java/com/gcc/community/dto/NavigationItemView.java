package com.gcc.community.dto;

/** 一个导航菜单项。 */
public record NavigationItemView(
        long id,
        String location,
        String title,
        String targetType,
        // 跳到页面时填页面 ID，跳外链时是 null
        String targetPageId,
        // 跳外链时填网址，跳页面时是空串
        String url,
        int sortOrder,
        boolean visible) {
}
