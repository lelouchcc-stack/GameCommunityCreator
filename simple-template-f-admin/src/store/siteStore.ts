import { defineStore } from "pinia";
import { ref } from "vue";
import { getSiteConfig } from "../api/site";
import { getUploadConfig } from "../api/upload";
import { setAssetBaseUrl } from "../utils/asset";
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
        // 存储基址决定「库里的 key 能不能显示成图片」，没它图片字段预览不出来
        const [config, storage] = await Promise.all([
            getSiteConfig(),
            getUploadConfig().catch(() => null),
        ]);
        if (storage) {
            setAssetBaseUrl(storage.publicBaseUrl);
        }
        apply(config);
    }

    return { theme, apply, load };
});

export default useSiteStore;
