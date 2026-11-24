package com.gcc.community.dto;

import java.time.LocalDateTime;

/** 社区项目的详情。 */
public record ProjectView(
        long id,
        String name,
        String slug,
        String gameName,
        String description,
        String status,
        // 首页的页面 ID
        String homePageId,
        // 首页草稿版本号
        int draftVersion,
        // 首页发布过的最新版本号，没发布过就是 null
        Integer publishedVersion,
        // 页面总数
        long pageCount,
        // 已经发布的页面数
        long publishedPageCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
