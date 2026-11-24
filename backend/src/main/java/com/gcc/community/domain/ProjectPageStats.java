package com.gcc.community.domain;

/**
 * 项目信息和页面统计，查一次就能拼出完整数据。
 */
public record ProjectPageStats(
        CommunityProject project,
        String homePageId,          // 首页 ID，正常不会缺
        int draftVersion,           // 首页草稿版本
        Integer publishedVersion,   // 没发布过就是 null
        long pageCount,             // 页面总数
        long publishedPageCount) {  // 已发布的页面数
}
