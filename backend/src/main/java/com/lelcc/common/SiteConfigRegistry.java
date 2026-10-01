package com.lelcc.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 站点配置项注册表：集中声明每个 config_key 的默认值与校验规则。
 * 读配置时用默认值补齐缺失键（前端永远拿到完整对象），写配置时逐项校验，
 * 避免配置键以魔法字符串的形式散落在各处。
 */
public final class SiteConfigRegistry {

    public static final String SITE_NAME = "site_name";
    public static final String SITE_INTRO = "site_intro";
    public static final String LOGO = "logo";
    public static final String COPYRIGHT = "copyright";
    public static final String PRIMARY_COLOR = "primary_color";
    public static final String DARK_MODE = "dark_mode";

    private static final Pattern COLOR = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
    private static final Pattern BOOLEAN = Pattern.compile("^(true|false)$");
    private static final Pattern IMAGE_URL = Pattern.compile("^(https?://|/).+");

    /** 顺序即后台表单的展示顺序 */
    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    static {
        put(SITE_NAME, "站点名称", "游戏社区", 30, null, true);
        put(SITE_INTRO, "站点简介", "", 200, null, false);
        put(LOGO, "站点 Logo", "/static/logo.png", 255, IMAGE_URL, false);
        put(COPYRIGHT, "版权文案", "© 2026 游戏社区", 100, null, false);
        put(PRIMARY_COLOR, "主题主色", "#3B82F6", 20, COLOR, true);
        put(DARK_MODE, "默认深色模式", "false", 5, BOOLEAN, true);
    }

    private SiteConfigRegistry() {
    }

    private static void put(String key, String label, String defaultValue,
                            int maxLength, Pattern pattern, boolean required) {
        SPECS.put(key, new Spec(key, label, defaultValue, maxLength, pattern, required));
    }

    public static List<Spec> specs() {
        return List.copyOf(SPECS.values());
    }

    public static Spec get(String key) {
        return SPECS.get(key);
    }

    public record Spec(String key, String label, String defaultValue,
                       int maxLength, Pattern pattern, boolean required) {

        /** 校验待写入的值；通过返回 null，否则返回可直接展示给管理员的中文提示 */
        public String validate(String value) {
            if (value == null || (required && value.isBlank())) {
                return label + "不能为空";
            }
            if (value.length() > maxLength) {
                return label + "不能超过 " + maxLength + " 个字符";
            }
            if (pattern != null && !value.isBlank() && !pattern.matcher(value).matches()) {
                return PRIMARY_COLOR.equals(key) ? "颜色值格式非法，请重新选择" : label + "格式不合法";
            }
            return null;
        }
    }
}
