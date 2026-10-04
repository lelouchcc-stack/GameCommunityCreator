package com.lelcc.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 组件属性库（属性元数据的唯一事实源），是前端 propertyCatalog 的 Java 镜像。
 * 组件类型通过 style/content/behavior_schema 勾选开放哪些 key，
 * 这里负责校验节点上填的值是否合法——白名单优先，未知 key 直接拒绝。
 */
public final class ComponentPropertyRegistry {

    public static final String TYPE_LENGTH = "length";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_COLOR = "color";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_URL = "url";
    public static final String TYPE_ENUM = "enum";
    public static final String TYPE_BOOL = "bool";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_BINDING = "binding";
    public static final String TYPE_JSON_ARRAY = "jsonArray";
    public static final String TYPE_JSON_OBJECT = "jsonObject";

    private static final Pattern LENGTH = Pattern.compile("^(auto|0|\\d+(\\.\\d+)?(px|%|rem|em|vh|vw))$");
    /** 允许 3/6/8 位 hex 与 rgb()/rgba()：颜色选择器开透明度后产出的就是这些形态 */
    private static final Pattern COLOR = Pattern.compile(
            "^(#[0-9a-fA-F]{3}|#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8}"
                    + "|rgba?\\([0-9.,%\\s]+\\)"
                    + "|var\\(--site-[a-z0-9-]+\\))$");
    private static final Pattern URL = Pattern.compile("^(https?://|/).+");
    /**
     * 图片值有两类：完整地址（http(s):// 或 / 开头，兼容历史数据与外链），
     * 或对象存储的 objectKey（upload/yyyy/MM/dd/<uuid>.<ext>，见 图片上传-存储抽象设计.json）。
     * key 按真实命名规则校验，顺带排除 .. 之类的路径穿越写法。
     */
    private static final Pattern IMAGE = Pattern.compile(
            "^(https?://|/).+$|^upload/\\d{4}/\\d{2}/\\d{2}/[A-Za-z0-9_-]{1,64}\\.[A-Za-z0-9]{1,8}$");
    private static final Pattern BINDING = Pattern.compile("^(\\$ref\\.[A-Za-z0-9_]+\\.[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)?|\\$runtime\\.[A-Za-z0-9_.]+)$");
    /** 危险字符：出现即拒绝，避免 CSS/HTML 注入 */
    private static final Pattern DANGER = Pattern.compile("[{};<>\u0000]");

    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    static {
        // 布局
        put("width", TYPE_LENGTH, "宽度");
        put("height", TYPE_LENGTH, "高度");
        put("maxWidth", TYPE_LENGTH, "最大宽度");
        put("padding", TYPE_LENGTH, "内边距");
        put("marginTop", TYPE_LENGTH, "上外边距");
        put("marginBottom", TYPE_LENGTH, "下外边距");
        putEnum("align", "水平对齐", "left", "center", "right", "full");
        putEnum("flexDirection", "主轴方向", "row", "column");
        putEnum("justify", "主轴对齐", "start", "center", "end", "space-between", "space-around");
        putEnum("alignItems", "交叉轴对齐", "start", "center", "end", "stretch");
        put("gap", TYPE_LENGTH, "间距");
        put("wrap", TYPE_BOOL, "自动换行");
        // 外观
        put("backgroundColor", TYPE_COLOR, "背景色");
        put("backgroundImage", TYPE_IMAGE, "背景图");
        putEnum("backgroundSize", "背景图填充", "cover", "contain", "auto");
        putRange("opacity", "不透明度", 0, 1);
        putEnum("texture", "质感", "none", "frosted", "transparent", "glass");
        put("borderWidth", TYPE_LENGTH, "边框粗细");
        putEnum("borderStyle", "边框样式", "solid", "dashed", "none");
        put("borderColor", TYPE_COLOR, "边框颜色");
        put("radius", TYPE_LENGTH, "圆角");
        // 文字
        put("fontSize", TYPE_LENGTH, "字号");
        putEnum("fontWeight", "字重", "400", "500", "600", "700");
        put("textColor", TYPE_COLOR, "文字颜色");
        putEnum("textAlign", "文字对齐", "left", "center", "right");
        putRange("lineHeight", "行高", 1, 3);
        put("letterSpacing", TYPE_LENGTH, "字间距");
        // 效果
        putEnum("boxShadow", "阴影", "none", "sm", "md", "lg", "glow");
        putEnum("hoverEffect", "悬停效果", "none", "lift", "glow", "scale", "underline");
        putEnum("clickEffect", "点击效果", "none", "scale", "press", "ripple", "opacity");
        putRange("transitionMs", "过渡时长", 0, 1000);
        putEnum("cursor", "鼠标指针", "default", "pointer", "text");
        // 内容
        putText("text", "文本内容", 200);
        putText("title", "标题", 100);
        putEnum("linkType", "跳转类型", "none", "internal", "external");
        put("linkUrl", TYPE_URL, "跳转地址");
        put("openInNewTab", TYPE_BOOL, "新窗口打开");
        put("imageUrl", TYPE_IMAGE, "图片地址");
        putEnum("imageFit", "图片填充", "cover", "contain", "fill");
        put("icon", TYPE_IMAGE, "图标地址");
        putRange("limit", "数量", 1, 20);
        putRange("columns", "列数", 1, 4);
        put("showCover", TYPE_BOOL, "显示封面");
        put("showIntro", TYPE_BOOL, "显示简介");
        put("autoplay", TYPE_BOOL, "自动播放");
        putRange("interval", "播放间隔", 1000, 10000);
        put("items", TYPE_JSON_ARRAY, "结构数组");
        put("itemsSource", TYPE_BINDING, "列表数据源");
        put("itemsTemplate", TYPE_JSON_ARRAY, "列表项模板");
        putText("emptyText", "空状态文案", 40);
        put("visibleWhen", TYPE_BINDING, "可见条件");
        // 表单类业务组件（登录 / 注册）的文案与跳转
        putText("submitText", "按钮文案", 20);
        putText("usernamePlaceholder", "用户名占位提示", 50);
        putText("passwordPlaceholder", "密码占位提示", 50);
        putText("nicknamePlaceholder", "昵称占位提示", 50);
        putText("redirectTo", "成功后跳转", 100);
        put("showRegisterLink", TYPE_BOOL, "显示注册入口");
        put("showLoginLink", TYPE_BOOL, "显示登录入口");
        // 行为
        putText("emit", "触发信号名", 40);
        put("onClickAction", TYPE_JSON_OBJECT, "点击动作");
        put("overlayClosable", TYPE_BOOL, "点击遮罩关闭");
        put("showClose", TYPE_BOOL, "显示关闭按钮");
        putEnum("animation", "弹窗动画", "fade", "zoom", "slide-down");
        putRange("zIndex", "层级", 1000, 9999);
    }

    private ComponentPropertyRegistry() {
    }

    private static void put(String key, String type, String label) {
        SPECS.put(key, new Spec(key, type, label, null, null, null, null));
    }

    private static void putEnum(String key, String label, String... options) {
        SPECS.put(key, new Spec(key, TYPE_ENUM, label, List.of(options), null, null, null));
    }

    private static void putRange(String key, String label, double min, double max) {
        SPECS.put(key, new Spec(key, TYPE_NUMBER, label, null, min, max, null));
    }

    private static void putText(String key, String label, int maxLength) {
        SPECS.put(key, new Spec(key, TYPE_TEXT, label, null, null, null, maxLength));
    }

    public static boolean isKnown(String key) {
        return SPECS.containsKey(key);
    }

    /**
     * 按类型校验一个属性值；通过返回 null，否则返回可直接展示的中文提示。
     */
    public static String validate(String key, Object value) {
        Spec spec = SPECS.get(key);
        if (spec == null) {
            return "未知属性：" + key;
        }
        if (value == null) {
            return null;
        }
        String text = value instanceof String s ? s : null;
        if (text != null && text.isBlank()) {
            // 空串＝"未设置"：属性面板清空某项时会送出空值，不应因此拦住整次保存
            return null;
        }
        if (text != null && DANGER.matcher(text).find()) {
            return spec.label() + "包含不允许的字符";
        }
        return switch (spec.type()) {
            case TYPE_LENGTH -> text != null && LENGTH.matcher(text).matches() ? null : spec.label() + "格式不合法";
            case TYPE_COLOR -> text != null && COLOR.matcher(text).matches() ? null : "颜色值格式非法，请重新选择";
            case TYPE_IMAGE -> isEmpty(text) || IMAGE.matcher(text).matches() ? null : spec.label() + "格式不合法";
            case TYPE_URL -> isEmpty(text) || URL.matcher(text).matches() ? null : spec.label() + "格式不合法";
            case TYPE_BINDING -> isEmpty(text) || BINDING.matcher(text).matches() ? null : spec.label() + "绑定格式不合法";
            case TYPE_ENUM -> text != null && spec.options().contains(text) ? null : spec.label() + "取值不在允许范围内";
            case TYPE_BOOL -> value instanceof Boolean ? null : spec.label() + "必须是布尔值";
            case TYPE_TEXT -> text != null && (spec.maxLength() == null || text.length() <= spec.maxLength())
                    ? null : spec.label() + "不能超过 " + spec.maxLength() + " 个字";
            case TYPE_NUMBER -> checkNumber(spec, value);
            case TYPE_JSON_ARRAY -> value instanceof List ? null : spec.label() + "必须是数组";
            case TYPE_JSON_OBJECT -> value instanceof Map ? null : spec.label() + "必须是对象";
            default -> "未知属性类型";
        };
    }

    private static String checkNumber(Spec spec, Object value) {
        Double num;
        if (value instanceof Number n) {
            num = n.doubleValue();
        } else if (value instanceof String s) {
            try {
                num = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return spec.label() + "必须是数字";
            }
        } else {
            return spec.label() + "必须是数字";
        }
        if (spec.min() != null && num < spec.min()) {
            return spec.label() + "不能小于 " + trim(spec.min());
        }
        if (spec.max() != null && num > spec.max()) {
            return spec.label() + "不能大于 " + trim(spec.max());
        }
        return null;
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }

    private record Spec(String key, String type, String label, List<String> options,
                        Double min, Double max, Integer maxLength) {
    }
}
