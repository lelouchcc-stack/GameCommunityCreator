package com.gcc.community.domain;

/**
 * 制作组成员。
 */
public record TeamMember(
        long id,
        long projectId,
        String name,
        String avatarUrl,
        String role,
        String bio,
        int sortOrder) {
}
