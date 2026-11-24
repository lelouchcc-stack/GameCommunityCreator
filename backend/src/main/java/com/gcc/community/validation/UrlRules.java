package com.gcc.community.validation;

import java.net.URI;
import java.net.URISyntaxException;

// 校验 URL 是不是合法的 http/https 绝对地址。
public final class UrlRules {

    public static final int MAX_URL_LENGTH = 300;

    private UrlRules() {
    }

    // 判断是不是合法的 http/https 绝对地址
    public static boolean isValidHttpUrl(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.strip();
        if (trimmed.isEmpty() || trimmed.length() > MAX_URL_LENGTH) {
            return false;
        }
        // 相对地址和带空格的都不行
        if (trimmed.startsWith("//") || trimmed.contains(" ")) {
            return false;
        }
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            String lower = scheme.toLowerCase(java.util.Locale.ROOT);
            if (!"http".equals(lower) && !"https".equals(lower)) {
                return false;
            }
            return uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}
