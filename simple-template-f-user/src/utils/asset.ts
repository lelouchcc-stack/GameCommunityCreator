/**
 * 图片地址解析（管理端与用户端同源副本，改动请两边同步）。
 *
 * 库里存的是 objectKey（如 upload/2026/10/04/x.png），或完整地址（http(s):// / 开头的历史数据与外链）。
 * 存储基址由 siteStore 在拿到配置后调 setAssetBaseUrl 注入，渲染时把 key 现拼成可访问地址——
 * 换存储、换 CDN 都不用迁移数据。
 */
let baseUrl = "";

export function setAssetBaseUrl(base?: string | null) {
    baseUrl = (base ?? "").trim();
}

export function resolveAssetUrl(value?: string | null): string {
    const raw = (value ?? "").trim();
    if (!raw) {
        return "";
    }
    if (/^https?:\/\//i.test(raw) || raw.startsWith("/")) {
        return raw;
    }
    if (!baseUrl) {
        return raw;
    }
    const base = baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;
    return `${base}/${raw.replace(/^\/+/, "")}`;
}
