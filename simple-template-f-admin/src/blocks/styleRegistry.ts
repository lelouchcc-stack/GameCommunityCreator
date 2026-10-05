/**
 * 样式值 -> CSS 的映射（管理端与用户端同源副本）。
 * 后端只校验值的合法性，这里负责把值变成样式。
 *
 * 约定：
 * - var(--site-xxx) 会补上兜底色，管理端没注入中性色 token 时也不会失效
 * - 质感/阴影/悬停/点击效果走预置类名（ui-blocks.css），其余走内联样式
 * - 业务组件（builtin:*）的「多区域样式」不落在外层盒子上，而是输出成 --w-* CSS 变量，
 *   由组件内部各区域用 var(--w-xxx, 默认值) 消费，这样标题/元信息/封面等可分别调
 */
import { resolveAssetUrl } from "../utils/asset";

type StyleMap = Record<string, any>

const TOKEN_FALLBACK: Record<string, string> = {
    "--site-primary": "#3B82F6",
    "--site-bg-card": "#FFFFFF",
    "--site-bg-page": "#F7F8FA",
    "--site-text-primary": "#1F2329",
    "--site-text-secondary": "#646A73",
    "--site-border": "#E5E6EB",
};

/** 给 var(--site-x) 补兜底色；其余值原样返回 */
function colorValue(value: unknown): string | undefined {
    if (typeof value !== "string" || !value) {
        return undefined;
    }
    const match = /^var\((--site-[a-z0-9-]+)\)$/.exec(value);
    if (match) {
        const token = match[1];
        if (!token) {
            return value;
        }
        const fallback = TOKEN_FALLBACK[token];
        return fallback ? `var(${token}, ${fallback})` : value;
    }
    return value;
}

function urlValue(value: unknown): string | undefined {
    if (typeof value !== "string" || !value) {
        return undefined;
    }
    // 值可能是对象存储的 key：先按存储基址现拼成可访问地址再交给 CSS
    const url = resolveAssetUrl(value);
    return url ? `url("${url}")` : undefined;
}

/* ---------------- 多区域样式规格 ---------------- */

export interface RegionStyleSpec {
    key: string
    label: string
    group: string
    type: "length" | "color" | "enum" | "number"
    options?: string[]
    min?: number
    max?: number
    step?: number
}

/**
 * 业务组件的分区域样式键。组件类型在 style_schema 里勾选要开放哪些，
 * 属性面板按 group 分组展示；值最终转成 --w-* 变量供组件内部消费。
 */
export const REGION_STYLE_SPECS: RegionStyleSpec[] = [
    { key: "itemPadding", label: "列表项内边距", group: "列表项", type: "length" },
    { key: "itemGap", label: "列表项间距", group: "列表项", type: "length" },
    { key: "itemBorderColor", label: "列表项分隔线", group: "列表项", type: "color" },
    { key: "itemHoverBackgroundColor", label: "悬停背景色", group: "列表项", type: "color" },
    { key: "titleColor", label: "标题颜色", group: "标题", type: "color" },
    { key: "titleFontSize", label: "标题字号", group: "标题", type: "length" },
    { key: "titleFontWeight", label: "标题字重", group: "标题", type: "enum", options: ["400", "500", "600", "700"] },
    { key: "metaColor", label: "元信息颜色", group: "元信息", type: "color" },
    { key: "metaFontSize", label: "元信息字号", group: "元信息", type: "length" },
    { key: "metaGap", label: "元信息间距", group: "元信息", type: "length" },
    { key: "coverWidth", label: "封面宽度", group: "封面", type: "length" },
    { key: "coverHeight", label: "封面高度", group: "封面", type: "length" },
    { key: "coverRadius", label: "封面圆角", group: "封面", type: "length" },
    { key: "coverFit", label: "封面填充", group: "封面", type: "enum", options: ["cover", "contain", "fill"] },
    { key: "rankColor", label: "序号颜色", group: "序号角标", type: "color" },
    { key: "rankFontSize", label: "序号字号", group: "序号角标", type: "length" },
    { key: "badgeBackgroundColor", label: "角标背景", group: "序号角标", type: "color" },
    { key: "badgeTextColor", label: "角标文字", group: "序号角标", type: "color" },
    { key: "emptyColor", label: "空状态颜色", group: "空状态", type: "color" },
    { key: "emptyFontSize", label: "空状态字号", group: "空状态", type: "length" },
    { key: "bodyColor", label: "正文字颜色", group: "正文", type: "color" },
    { key: "bodyFontSize", label: "正文字号", group: "正文", type: "length" },
    { key: "bodyLineHeight", label: "正文字行高", group: "正文", type: "number", min: 1, max: 3, step: 0.1 },
    { key: "actionColor", label: "动作文字色", group: "动作", type: "color" },
    { key: "actionBackgroundColor", label: "动作背景", group: "动作", type: "color" },
    { key: "actionFontSize", label: "动作字号", group: "动作", type: "length" },
    { key: "actionRadius", label: "动作圆角", group: "动作", type: "length" },
];

const REGION_SPEC_MAP = new Map(REGION_STYLE_SPECS.map((spec) => [spec.key, spec]));

export function regionSpecOf(key: string): RegionStyleSpec | undefined {
    return REGION_SPEC_MAP.get(key);
}

/** 区域键 -> CSS 变量名，如 titleColor -> --w-title-color */
export function regionCssVar(key: string): string {
    return "--w-" + key.replace(/[A-Z]/g, (m) => "-" + m.toLowerCase());
}

/** 内容/样式里出现过的区域键（供组件按需读取） */
export function regionKeys(): string[] {
    return REGION_STYLE_SPECS.map((spec) => spec.key);
}

/** 内联样式映射：属性 key -> CSS 属性名 */
const INLINE: Record<string, string> = {
    width: "width",
    height: "height",
    maxWidth: "maxWidth",
    padding: "padding",
    marginTop: "marginTop",
    marginBottom: "marginBottom",
    backgroundSize: "backgroundSize",
    opacity: "opacity",
    borderWidth: "borderWidth",
    borderStyle: "borderStyle",
    radius: "borderRadius",
    fontSize: "fontSize",
    fontWeight: "fontWeight",
    textAlign: "textAlign",
    lineHeight: "lineHeight",
    letterSpacing: "letterSpacing",
    flexDirection: "flexDirection",
    justify: "justifyContent",
    alignItems: "alignItems",
    gap: "gap",
    cursor: "cursor",
};

export function toCss(style: StyleMap | null | undefined): Record<string, string> {
    const css: Record<string, string> = {};
    if (!style) {
        return css;
    }
    for (const [key, raw] of Object.entries(style)) {
        if (raw === null || raw === undefined || raw === "") {
            continue;
        }
        const regionSpec = REGION_SPEC_MAP.get(key);
        if (regionSpec) {
            if (regionSpec.type === "color") {
                const value = colorValue(raw);
                if (value) {
                    css[regionCssVar(key)] = value;
                }
            } else {
                css[regionCssVar(key)] = String(raw);
            }
            continue;
        }
        if (key === "align") {
            if (raw === "center") {
                css.marginLeft = "auto";
                css.marginRight = "auto";
            } else if (raw === "right") {
                css.marginLeft = "auto";
            } else if (raw === "full") {
                css.width = "100%";
            }
            continue;
        }
        if (key === "backgroundColor") {
            const value = colorValue(raw);
            if (value) {
                css.backgroundColor = value;
            }
            continue;
        }
        if (key === "textColor") {
            const value = colorValue(raw);
            if (value) {
                css.color = value;
            }
            continue;
        }
        if (key === "borderColor") {
            const value = colorValue(raw);
            if (value) {
                css.borderColor = value;
            }
            continue;
        }
        if (key === "backgroundImage") {
            const value = urlValue(raw);
            if (value) {
                css.backgroundImage = value;
            }
            continue;
        }
        if (key === "wrap") {
            css.flexWrap = raw === true || raw === "true" ? "wrap" : "nowrap";
            continue;
        }
        if (key === "transitionMs") {
            css["--ui-transition"] = `${raw}ms`;
            continue;
        }
        const cssKey = INLINE[key];
        if (cssKey) {
            css[cssKey] = typeof raw === "boolean" ? String(raw) : String(raw);
        }
    }
    return css;
}

/** 预置类名：质感 / 阴影 / 悬停 / 点击效果 */
export function toClasses(style: StyleMap | null | undefined): string[] {
    const classes: string[] = [];
    if (!style) {
        return classes;
    }
    const pairs: [string, string][] = [
        ["texture", "ui-texture-"],
        ["boxShadow", "ui-shadow-"],
        ["hoverEffect", "ui-hover-"],
        ["clickEffect", "ui-click-"],
    ];
    for (const [key, prefix] of pairs) {
        const value = style[key];
        if (typeof value === "string" && value && value !== "none") {
            classes.push(prefix + value);
        }
    }
    return classes;
}
