package com.gcc.community.domain;

/**
 * 游戏信息，一个项目一条。
 */
public record GameInfo(
        long projectId,
        String name,
        String tagline,
        String description,
        String coverUrl,
        String platforms,
        String releaseStatus) {
}
