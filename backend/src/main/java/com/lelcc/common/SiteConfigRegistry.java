package com.lelcc.common;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 站点配置项注册表：集中声明每个 config_key 的默认值与校验规则。
 * 读配置时用默认值补齐缺失键（前端永远拿到完整对象），写配置时逐项校验，
 * 避免配置键以魔法字符串的形式散落在各处。
 *
 * <p>这就是「改站名即可运行」的落点：模板与页面不写死任何社区名/游戏名，
 * 全部从这里声明的键读出来。新增一个纯值型配置项只需在下面加一行、
 * 在 SiteConfigVO/DTO 各加一个字段、样式里 var() 引用即可。</p>
 */
public final class SiteConfigRegistry {

    public static final String SITE_NAME = "site_name";
    public static final String GAME_NAME = "game_name";
    public static final String SITE_INTRO = "site_intro";
    public static final String LOGO = "logo";
    public static final String NAV_ITEMS = "nav_items";
    public static final String COPYRIGHT = "copyright";
    public static final String PRIMARY_COLOR = "primary_color";
    public static final String DARK_MODE = "dark_mode";

    private static final Pattern COLOR = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
    private static final Pattern BOOLEAN = Pattern.compile("^(true|false)$");
    /** 图片值：完整地址（http(s):// 或 / 开头），或对象存储 objectKey（upload/yyyy/MM/dd/<uuid>.<ext>） */
    private static final Pattern IMAGE = Pattern.compile(
            "^(https?://|/).+$|^upload/\\d{4}/\\d{2}/\\d{2}/[A-Za-z0-9_-]{1,64}\\.[A-Za-z0-9]{1,8}$");

    /** 导航项的字段上限：超了会让页头在窄屏溢出，所以在入口就挡住 */
    private static final int NAV_MAX_ITEMS = 8;
    private static final int NAV_LABEL_MAX = 12;
    private static final int NAV_URL_MAX = 100;

    /** 顺序即后台表单的展示顺序 */
    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    /**
     * 默认导航：指向默认模板建出来的四个入口，任何页面都不存在时它们会各自给出 404 占位，
     * 不会白屏。社区改过导航后以配置为准。
     */
    public static final String DEFAULT_NAV_JSON =
            "[{\"label\":\"首页\",\"url\":\"/\"},{\"label\":\"攻略与资料\",\"url\":\"/guide\"},"
                    + "{\"label\":\"社区板块\",\"url\":\"/boards\"},{\"label\":\"公告\",\"url\":\"/notice\"}]";

    static {
        put(SITE_NAME, "站点名称", "游戏社区", 30, null, true);
        put(GAME_NAME, "游戏名称", "", 30, null, false);
        put(SITE_INTRO, "站点简介", "", 200, null, false);
        put(LOGO, "站点 Logo", "", 255, IMAGE, false);
        putJson(NAV_ITEMS, "导航菜单", DEFAULT_NAV_JSON, 900, true);
        put(COPYRIGHT, "版权文案", "", 100, null, false);
        put(PRIMARY_COLOR, "主题主色", "#5B5FE9", 20, COLOR, true);
        put(DARK_MODE, "默认深色模式", "false", 5, BOOLEAN, true);
    }

    private SiteConfigRegistry() {
    }

    private static void put(String key, String label, String defaultValue,
                            int maxLength, Pattern pattern, boolean required) {
        SPECS.put(key, new Spec(key, label, defaultValue, maxLength, pattern, required, false));
    }

    /** 结构化的配置项（当前只有导航菜单）：值是一段 JSON，按下面的规则逐项校验 */
    private static void putJson(String key, String label, String defaultValue,
                                int maxLength, boolean required) {
        SPECS.put(key, new Spec(key, label, defaultValue, maxLength, null, required, true));
    }

    public static List<Spec> specs() {
        return List.copyOf(SPECS.values());
    }

    public static Spec get(String key) {
        return SPECS.get(key);
    }

    /** 读导航菜单：配置坏掉或为空时回落到默认导航，让页头永远是可用的 */
    public static JSONArray parseNavItems(String value) {
        if (value == null || value.isBlank()) {
            return JSONUtil.parseArray(DEFAULT_NAV_JSON);
        }
        try {
            JSONArray array = JSONUtil.parseArray(value);
            return array.isEmpty() ? JSONUtil.parseArray(DEFAULT_NAV_JSON) : array;
        } catch (Exception e) {
            return JSONUtil.parseArray(DEFAULT_NAV_JSON);
        }
    }

    public record Spec(String key, String label, String defaultValue,
                       int maxLength, Pattern pattern, boolean required, boolean json) {

        /** 校验待写入的值；通过返回 null，否则返回可直接展示给管理员的中文提示 */
        public String validate(String value) {
            if (value == null || (required && value.isBlank())) {
                return label + "不能为空";
            }
            if (value.length() > maxLength) {
                return label + "不能超过 " + maxLength + " 个字符";
            }
            if (json) {
                return value.isBlank() ? null : validateNavJson(value);
            }
            if (pattern != null && !value.isBlank() && !pattern.matcher(value).matches()) {
                return PRIMARY_COLOR.equals(key) ? "颜色值格式非法，请重新选择" : label + "格式不合法";
            }
            return null;
        }

        /**
         * 导航菜单的结构校验：数组、条数上限、每项都有 label/url、
         * url 必须是站内路径（/ 开头）或 http(s) 外链。返回错误文案或 null。
         */
        private String validateNavJson(String value) {
            JSONArray array;
            try {
                array = JSONUtil.parseArray(value);
            } catch (Exception e) {
                return label + "必须是合法的 JSON 数组";
            }
            if (array.size() > NAV_MAX_ITEMS) {
                return label + "最多 " + NAV_MAX_ITEMS + " 项";
            }
            for (Object item : array) {
                if (!(item instanceof JSONObject node)) {
                    return label + "每一项都必须是 {label, url} 对象";
                }
                String labelValue = node.getStr("label", "");
                String url = node.getStr("url", "");
                if (labelValue.isBlank()) {
                    return label + "里有项目缺名称";
                }
                if (labelValue.length() > NAV_LABEL_MAX) {
                    return label + "的名称不能超过 " + NAV_LABEL_MAX + " 个字";
                }
                if (url.length() > NAV_URL_MAX) {
                    return label + "的地址过长";
                }
                if (!url.startsWith("/") && !url.startsWith("http://") && !url.startsWith("https://")) {
                    return label + "的地址必须是 / 开头的站内路径或 http(s) 外链";
                }
            }
            return null;
        }
    }
}
