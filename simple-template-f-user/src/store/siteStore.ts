import { defineStore } from "pinia";
import { ref } from "vue";
import { getSiteBundle } from "../api/site";
import { applySiteTheme } from "../utils/theme";
import type { PageTree, SiteConfig } from "../types/site";

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
    /** 后端只返回启用页面的可见节点，且样式已与组件默认值合并，前端直接渲染 */
    const homePage = ref<PageTree | null>(null);
    const loaded = ref(false);

    async function loadConfig() {
        const bundle = await getSiteBundle();
        theme.value = bundle.theme;
        homePage.value = bundle.homePage;
        applySiteTheme(bundle.theme, { title: true, favicon: true });
        localStorage.setItem(THEME_CACHE_KEY, JSON.stringify(bundle.theme));
        loaded.value = true;
    }

    return { theme, homePage, loaded, loadConfig };
});

export default useSiteStore;
