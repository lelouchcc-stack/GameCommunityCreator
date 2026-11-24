package com.gcc.community.dto;

/** 制作组成员的信息。 */
public record TeamMemberView(
        long id,
        String name,
        String avatarUrl,
        String role,
        String bio,
        int sortOrder) {
}
