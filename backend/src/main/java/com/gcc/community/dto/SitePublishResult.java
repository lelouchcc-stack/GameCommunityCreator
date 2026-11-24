package com.gcc.community.dto;

import java.util.List;

/** 站点发布的返回结果。 */
public record SitePublishResult(
        List<PublishPageResult> publishedPages,
        List<SitePublishSkipped> skippedPages) {
}
