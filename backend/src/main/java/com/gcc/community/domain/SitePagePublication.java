package com.gcc.community.domain;

import java.time.LocalDateTime;

/**
 * 页面发布时的快照，公开端只读这里。
 */
public record SitePagePublication(
        long id,
        String pageId,
        long projectId,
        int configVersion,
        String configJson,
        LocalDateTime publishedAt) {
}
