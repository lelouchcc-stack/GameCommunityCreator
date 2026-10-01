import { defineStore } from "pinia";
import { ref } from "vue";
import { getSiteConfig } from "../api/site";
import { applySiteTheme } from "../utils/theme";
import type { SiteConfig } from "../types/site";

const useSiteStore = defineStore("site", () => {
    const theme = ref<SiteConfig | null>(null);

    /** 应用并记住当前主题；保存成功后把接口返回的最新配置交给它即可即时预览 */
    function apply(next: SiteConfig) {
        theme.value = next;
        applySiteTheme(next);
    }

    async function load() {
        apply(await getSiteConfig());
    }

    return { theme, apply, load };
});

export default useSiteStore;
