package com.gcc.community.dto;

/** 新增或修改制作组成员的请求参数。 */
public record TeamMemberRequest(
        // 名字，新建时必填
        String name,
        String avatarUrl,
        String role,
        String bio,
        Integer sortOrder) {
}
