package com.gcc.community.domain;

import java.time.LocalDateTime;

/**
 * 开发者账号。
 */
public record AppUser(
        long id,
        String username,
        String passwordHash,    // BCrypt 哈希，不返回给前端
        String displayName,
        LocalDateTime createdAt) {
}
