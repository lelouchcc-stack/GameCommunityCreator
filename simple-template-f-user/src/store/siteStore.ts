import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { getSiteBundle } from "../api/site";
import { resolveAssetUrl, setAssetBaseUrl } from "../utils/asset";
import { applySiteTheme } from "../utils/theme";
import type { LogicGraph, PageEntry, SiteConfig } from "../types/site";

const THEME_CACHE_KEY = "site_theme";

/** 读上次缓存的主题：main.ts 在挂载前用它先注入变量，避免等接口时闪一下默认色 */
export function readCachedTheme(): SiteConfig | null {
    try {
        const raw = localStorage.getItem(THEME_CACHE_KEY);
        return raw ? (JSON.parse(raw) as SiteConfig) : null;
    } catch {
        return null;
    }
}

const useSiteStore = defineStore("site", () => {
    const theme = ref<SiteConfig | null>(null);
    /** 后端下发的所有启用页面：每页带自己的可见节点树与逻辑图 */
    const pages = ref<PageEntry[]>([]);
    /** 所有页的图引用到的预设（code@version -> 子图） */
    const presets = ref<Record<string, LogicGraph>>({});
    const loaded = ref(false);

    /** 首页页面：站点外壳（页头/页脚）按它渲染；没有标记首页时退回第一个页面 */
    const homePage = computed<PageEntry | null>(
        () => pages.value.find((page) => page.isHome === 1) ?? pages.value[0] ?? null,
    );

    /** 按路由地址找页面；找不到返回 null，调用方据此给 404 占位 */
    function pageByPath(path: string): PageEntry | null {
        const normalized = path.replace(/\/+$/, "") || "/";
        return pages.value.find((page) => (page.routePath ?? "") === normalized) ?? null;
    }

    async function loadConfig() {
        const bundle = await getSiteBundle();
        // 存储基址：页面节点与站点 Logo 里存的可能是 objectKey，渲染前要能现拼成地址
        setAssetBaseUrl(bundle.storage?.publicBaseUrl);
        // Logo 在这里就解析成可访问地址：页头图片、favicon 与本地缓存都直接可用
        const resolvedTheme: SiteConfig = { ...bundle.theme, logo: resolveAssetUrl(bundle.theme.logo) };
        theme.value = resolvedTheme;
        pages.value = bundle.pages ?? [];
        presets.value = bundle.presets ?? {};
        applySiteTheme(resolvedTheme, { title: true, favicon: true });
        localStorage.setItem(THEME_CACHE_KEY, JSON.stringify(resolvedTheme));
        loaded.value = true;
    }

    return { theme, pages, presets, loaded, homePage, pageByPath, loadConfig };
});

export default useSiteStore;
