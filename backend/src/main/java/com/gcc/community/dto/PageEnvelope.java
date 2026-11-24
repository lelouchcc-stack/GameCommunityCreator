package com.gcc.community.dto;

import java.time.LocalDateTime;

import com.gcc.community.domain.PageConfig;

/** 返回页面草稿和版本信息。 */
public record PageEnvelope(
        long projectId,
        int configVersion,
        LocalDateTime updatedAt,
        PageConfig config) {
}
