package com.gcc.community.dto;

/** 公开社区详情里的项目信息。 */
public record PublicProject(
        long id,
        String name,
        String slug,
        String gameName,
        String description) {
}
