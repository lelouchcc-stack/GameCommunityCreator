package com.gcc.community.domain;

import java.time.LocalDateTime;

/**
 * 发布历史里的一条记录。
 */
public record PagePublicationSummary(long publicationId, int configVersion, LocalDateTime publishedAt) {
}
