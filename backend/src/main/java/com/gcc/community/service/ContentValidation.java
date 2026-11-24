package com.gcc.community.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gcc.community.common.ApiException;

/** 内容模块共用的字段校验工具。 */
final class ContentValidation {

    private ContentValidation() {
    }

    // 必填，1 到 max 个字符
    static void requiredText(Map<String, String> errors, String field, String value, int max) {
        text(errors, field, value, 1, max);
    }

    // 可选，null 当空串处理
    static void optionalText(Map<String, String> errors, String field, String value, int max) {
        text(errors, field, value, 0, max);
    }

    static void text(Map<String, String> errors, String field, String value, int min, int max) {
        String actual = value == null ? "" : value;
        int length = actual.strip().length();
        if (length < min) {
            errors.put(field, "不能为空");
        } else if (actual.length() > max) {
            errors.put(field, "长度不能超过 " + max + " 个字符");
        }
    }

    static void requiredEnum(Map<String, String> errors, String field, String value, List<String> allowed) {
        if (value == null || value.isBlank()) {
            errors.put(field, "不能为空");
        } else if (!allowed.contains(value)) {
            errors.put(field, "只能是 " + String.join("、", allowed));
        }
    }

    static void optionalEnum(Map<String, String> errors, String field, String value, List<String> allowed) {
        if (value != null && !value.isBlank() && !allowed.contains(value)) {
            errors.put(field, "只能是 " + String.join("、", allowed));
        }
    }

    static void requirePositiveOrZero(Map<String, String> errors, String field, Integer value) {
        if (value != null && value < 0) {
            errors.put(field, "不能为负数");
        }
    }

    static void throwIfAny(String message, Map<String, String> errors) {
        if (errors.isEmpty()) {
            return;
        }
        Map.Entry<String, String> first = errors.entrySet().iterator().next();
        throw ApiException.badRequest(message + "：" + first.getKey() + " " + first.getValue(),
                new LinkedHashMap<>(errors));
    }

    static String orDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
