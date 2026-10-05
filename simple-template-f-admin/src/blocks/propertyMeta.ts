/**
 * 组件属性元数据（前端镜像 ComponentPropertyRegistry），仅管理端使用：
 * 属性面板与"默认值"编辑器据此动态生成控件。
 */
import { REGION_STYLE_SPECS } from "./styleRegistry";

export type PropType =
    | "length"
    | "number"
    | "color"
    | "image"
    | "url"
    | "enum"
    | "bool"
    | "text"
    | "binding"
    | "jsonArray"
    | "jsonObject";

export interface PropertyMeta {
    key: string
    label: string
    type: PropType
    options?: string[]
    min?: number
    max?: number
    step?: number
    maxLength?: number
}

export interface PropertyGroup {
    group: string
    label: string
    properties: PropertyMeta[]
}

const BASE_STYLE_GROUPS: PropertyGroup[] = [
    {
        group: "layout",
        label: "布局",
        properties: [
            { key: "width", label: "宽度", type: "length" },
            { key: "height", label: "高度", type: "length" },
            { key: "maxWidth", label: "最大宽度", type: "length" },
            { key: "padding", label: "内边距", type: "length" },
            { key: "marginTop", label: "上外边距", type: "length" },
            { key: "marginBottom", label: "下外边距", type: "length" },
            { key: "align", label: "水平对齐", type: "enum", options: ["left", "center", "right", "full"] },
            { key: "flexDirection", label: "主轴方向", type: "enum", options: ["row", "column"] },
            { key: "justify", label: "主轴对齐", type: "enum", options: ["start", "center", "end", "space-between", "space-around"] },
            { key: "alignItems", label: "交叉轴对齐", type: "enum", options: ["start", "center", "end", "stretch"] },
            { key: "gap", label: "间距", type: "length" },
            { key: "wrap", label: "自动换行", type: "bool" },
        ],
    },
    {
        group: "appearance",
        label: "外观",
        properties: [
            { key: "backgroundColor", label: "背景色", type: "color" },
            { key: "backgroundImage", label: "背景图", type: "image" },
            { key: "backgroundSize", label: "背景图填充", type: "enum", options: ["cover", "contain", "auto"] },
            { key: "opacity", label: "不透明度", type: "number", min: 0, max: 1, step: 0.05 },
            { key: "texture", label: "质感", type: "enum", options: ["none", "frosted", "transparent", "glass"] },
            { key: "borderWidth", label: "边框粗细", type: "length" },
            { key: "borderStyle", label: "边框样式", type: "enum", options: ["solid", "dashed", "none"] },
            { key: "borderColor", label: "边框颜色", type: "color" },
            { key: "radius", label: "圆角", type: "length" },
        ],
    },
    {
        group: "typography",
        label: "文字",
        properties: [
            { key: "fontSize", label: "字号", type: "length" },
            { key: "fontWeight", label: "字重", type: "enum", options: ["400", "500", "600", "700"] },
            { key: "textColor", label: "文字颜色", type: "color" },
            { key: "textAlign", label: "文字对齐", type: "enum", options: ["left", "center", "right"] },
            { key: "lineHeight", label: "行高", type: "number", min: 1, max: 3, step: 0.1 },
            { key: "letterSpacing", label: "字间距", type: "length" },
        ],
    },
    {
        group: "effect",
        label: "效果",
        properties: [
            { key: "boxShadow", label: "阴影", type: "enum", options: ["none", "sm", "md", "lg", "glow"] },
            { key: "hoverEffect", label: "悬停效果", type: "enum", options: ["none", "lift", "glow", "scale", "underline"] },
            { key: "clickEffect", label: "点击效果", type: "enum", options: ["none", "scale", "press", "ripple", "opacity"] },
            { key: "transitionMs", label: "过渡时长(ms)", type: "number", min: 0, max: 1000, step: 50 },
            { key: "cursor", label: "鼠标指针", type: "enum", options: ["default", "pointer", "text"] },
        ],
    },
];

/**
 * 业务组件的分区域样式（列表项/标题/元信息/封面…）按 group 归拢成属性分组。
 * 与 styleRegistry 的 REGION_STYLE_SPECS 同源，组件类型勾选哪些键由各自 style_schema 决定。
 */
function buildRegionGroups(): PropertyGroup[] {
    const groups = new Map<string, PropertyGroup>();
    for (const spec of REGION_STYLE_SPECS) {
        let group = groups.get(spec.group);
        if (!group) {
            group = { group: `region-${groups.size}`, label: spec.group, properties: [] };
            groups.set(spec.group, group);
        }
        group.properties.push({
            key: spec.key,
            label: spec.label,
            type: spec.type as PropType,
            options: spec.options,
            min: spec.min,
            max: spec.max,
            step: spec.step,
        });
    }
    return [...groups.values()];
}

export const STYLE_GROUPS: PropertyGroup[] = [...BASE_STYLE_GROUPS, ...buildRegionGroups()];

export const CONTENT_GROUP: PropertyGroup = {
    group: "content",
    label: "内容",
    properties: [
        { key: "text", label: "文本内容", type: "text", maxLength: 200 },
        { key: "title", label: "标题", type: "text", maxLength: 100 },
        { key: "linkType", label: "跳转类型", type: "enum", options: ["none", "internal", "external"] },
        { key: "linkUrl", label: "跳转地址", type: "url" },
        { key: "openInNewTab", label: "新窗口打开", type: "bool" },
        { key: "imageUrl", label: "图片地址", type: "image" },
        { key: "imageFit", label: "图片填充", type: "enum", options: ["cover", "contain", "fill"] },
        { key: "icon", label: "图标地址", type: "image" },
        { key: "limit", label: "数量", type: "number", min: 1, max: 20 },
        { key: "columns", label: "列数", type: "number", min: 1, max: 4 },
        { key: "showCover", label: "显示封面", type: "bool" },
        { key: "showIntro", label: "显示简介", type: "bool" },
        { key: "autoplay", label: "自动播放", type: "bool" },
        { key: "interval", label: "播放间隔(ms)", type: "number", min: 1000, max: 10000, step: 500 },
        { key: "items", label: "结构数组", type: "jsonArray" },
        { key: "itemsSource", label: "列表数据源", type: "binding" },
        { key: "itemsTemplate", label: "列表项模板", type: "jsonArray" },
        { key: "emptyText", label: "空状态文案", type: "text", maxLength: 40 },
        { key: "visibleWhen", label: "可见条件", type: "binding" },
        { key: "placeholder", label: "占位提示", type: "text", maxLength: 50 },
        { key: "inputType", label: "输入类型", type: "enum", options: ["text", "password", "number", "email"] },
        { key: "buttonType", label: "按钮类型", type: "enum", options: ["button", "submit"] },
        { key: "boardId", label: "板块 ID", type: "number", min: 1 },
        { key: "versionId", label: "版本 ID", type: "number", min: 1 },
        { key: "stageId", label: "进度阶段 ID", type: "number", min: 1 },
        { key: "tagId", label: "标签 ID", type: "number", min: 1 },
        { key: "postId", label: "帖子 ID", type: "number", min: 1 },
        { key: "keyword", label: "搜索关键字", type: "text", maxLength: 50 },
        { key: "sort", label: "排序方式", type: "enum", options: ["latest", "hot"] },
    ],
};

export const BEHAVIOR_GROUP: PropertyGroup = {
    group: "behavior",
    label: "行为",
    properties: [
        { key: "emit", label: "触发信号名", type: "text", maxLength: 40 },
        { key: "onClickAction", label: "点击动作", type: "jsonObject" },
        { key: "overlayClosable", label: "点击遮罩关闭", type: "bool" },
        { key: "showClose", label: "显示关闭按钮", type: "bool" },
        { key: "animation", label: "弹窗动画", type: "enum", options: ["fade", "zoom", "slide-down"] },
        { key: "zIndex", label: "层级", type: "number", min: 1000, max: 9999 },
    ],
};

export const ALL_GROUPS: PropertyGroup[] = [...STYLE_GROUPS, CONTENT_GROUP, BEHAVIOR_GROUP];

export const PROPERTY_MAP: Record<string, PropertyMeta> = {};
ALL_GROUPS.forEach((group) => {
    group.properties.forEach((property) => {
        PROPERTY_MAP[property.key] = property;
    });
});

/** 取属性元数据；属性库一定包含 schema 里的 key，这里兜底避免类型收窄成 undefined */
export function metaOf(key: string): PropertyMeta {
    return PROPERTY_MAP[key] ?? { key, label: key, type: "text" };
}

/**
 * 内容里可以绑到「图节点输出」的字段：填 $ref.<节点id>.<端口>，渲染器解析后取值。
 * 与 logic/valueRef 的求值范围一致（visibleWhen 是布尔绑定，itemsSource 供列表组件用）。
 */
export const BINDABLE_KEYS = new Set(["text", "title", "imageUrl", "icon", "itemsSource", "visibleWhen"]);

export const CATEGORY_LABELS: Record<string, string> = {
    base: "基础",
    layout: "布局",
    display: "展示",
    interactive: "交互",
    business: "业务",
};

/** 已注册的渲染内核（与后端 RENDER_KINDS 一致） */
export const RENDER_KINDS = [
    "text",
    "heading",
    "image",
    "button",
    "divider",
    "spacer",
    "container",
    "carousel",
    "list",
    "tabs",
    "modal",
    "icon",
    "input",
    "form",
];
