import { defineStore } from "pinia";
import { ref } from "vue";
import { getSiteBundle } from "../api/site";
import { applySiteTheme } from "../utils/theme";
import type { HomeModule, SiteConfig } from "../types/site";

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
    /** 后端只返回启用的模块且已按 sort 升序，前端不再排序 */
    const homeModules = ref<HomeModule[]>([]);
    const loaded = ref(false);

    async function loadConfig() {
        const bundle = await getSiteBundle();
        theme.value = bundle.theme;
        homeModules.value = bundle.homeModules;
        applySiteTheme(bundle.theme, { title: true, favicon: true });
        localStorage.setItem(THEME_CACHE_KEY, JSON.stringify(bundle.theme));
        loaded.value = true;
    }

    return { theme, homeModules, loaded, loadConfig };
});

export default useSiteStore;
