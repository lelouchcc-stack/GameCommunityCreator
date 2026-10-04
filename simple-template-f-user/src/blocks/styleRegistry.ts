/**
 * 样式值 -> CSS 的映射（管理端与用户端同源副本）。
 * 后端只校验值的合法性，这里负责把值变成样式。
 *
 * 约定：
 * - var(--site-xxx) 会补上兜底色，管理端没注入中性色 token 时也不会失效
 * - 质感/阴影/悬停/点击效果走预置类名（ui-blocks.css），其余走内联样式
 */
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
    return `url("${value}")`;
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
