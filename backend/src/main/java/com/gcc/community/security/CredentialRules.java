package com.gcc.community.security;

import java.util.List;
import java.util.regex.Pattern;

// 注册和 bootstrap 用的账号密码规则。
public final class CredentialRules {

    // 用户名：3-30 位字母、数字、下划线或中划线
    public static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,30}$");

    public static final int PASSWORD_MIN_LENGTH = 8;

    private CredentialRules() {
    }

    // 密码至少 8 位，并且要有字母和数字
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    // 把注册校验的错误放进 errors
    public static void validateRegistration(String username, String password, String displayName,
            java.util.Map<String, String> errors) {
        if (username == null || username.isBlank()) {
            errors.put("username", "用户名不能为空");
        } else if (!isValidUsername(username)) {
            errors.put("username", "用户名需为 3-30 位字母、数字、下划线或中划线");
        }
        if (password == null || password.isEmpty()) {
            errors.put("password", "密码不能为空");
        } else if (!isStrongPassword(password)) {
            errors.put("password", "密码至少 8 位，且同时包含字母与数字");
        }
        if (displayName == null || displayName.strip().isEmpty()) {
            errors.put("displayName", "显示名称不能为空");
        } else if (displayName.length() > 60) {
            errors.put("displayName", "显示名称长度不能超过 60 个字符");
        }
    }
}
