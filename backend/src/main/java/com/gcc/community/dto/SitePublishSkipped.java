package com.gcc.community.dto;

/** 站点发布时被跳过的页面。 */
public record SitePublishSkipped(String pageId, String reason) {
}
