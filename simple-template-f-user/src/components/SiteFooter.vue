<template>
    <footer class="site-footer">
        <div class="site-footer__inner">
            <div class="site-footer__about">
                <div class="site-footer__title">{{ siteName }}</div>
                <p v-if="siteIntro" class="site-footer__intro">{{ siteIntro }}</p>
            </div>

            <nav class="site-footer__nav" aria-label="页脚导航">
                <router-link
                    v-for="item in navItems"
                    :key="`f-${item.url}`"
                    class="site-footer__link"
                    :to="item.url"
                >
                    {{ item.label }}
                </router-link>
            </nav>
        </div>

        <div class="site-footer__bottom">
            <span class="site-footer__copyright">{{ copyright }}</span>
            <span v-if="gameName" class="site-footer__game">{{ gameName }}</span>
        </div>
    </footer>
</template>

<script lang="ts" setup>
/**
 * 站点页脚（用户端外壳的一部分）。版权与站点简介同样取站点配置，
 * 没有配置版权时按「站点名 + 年份」生成一条，不留空白区域。
 */
import { computed } from "vue";
import useSiteStore from "../store/siteStore";

const siteStore = useSiteStore();

const siteName = computed(() => siteStore.theme?.siteName || "游戏社区");
const gameName = computed(() => siteStore.theme?.gameName || "");
const siteIntro = computed(() => siteStore.theme?.siteIntro || "");

const navItems = computed(() => {
    const configured = siteStore.theme?.navItems ?? [];
    return configured.length
        ? configured
        : [
              { label: "首页", url: "/" },
              { label: "攻略与资料", url: "/guide" },
              { label: "社区板块", url: "/boards" },
              { label: "公告", url: "/notice" },
          ];
});

const copyright = computed(
    () => siteStore.theme?.copyright || `© ${new Date().getFullYear()} ${siteName.value}`,
);
</script>

<style scoped>
.site-footer {
    margin-top: auto;
    border-top: 1px solid var(--site-border);
    background-color: var(--site-footer-bg);
    color: var(--site-text-secondary);
}

.site-footer__inner {
    display: flex;
    flex-wrap: wrap;
    gap: var(--site-space-5);
    align-items: flex-start;
    justify-content: space-between;
    width: 100%;
    max-width: var(--site-container);
    margin: 0 auto;
    padding: var(--site-space-5) var(--site-gutter);
    box-sizing: border-box;
}

.site-footer__about {
    flex: 1 1 320px;
    min-width: 0;
}

.site-footer__title {
    font-size: 15px;
    font-weight: 700;
    color: var(--site-text-primary);
}

.site-footer__intro {
    max-width: 520px;
    margin: 6px 0 0;
    font-size: 13px;
    line-height: 1.7;
}

.site-footer__nav {
    display: flex;
    flex: none;
    flex-wrap: wrap;
    gap: var(--site-space-1) var(--site-space-4);
}

.site-footer__link {
    color: var(--site-text-secondary);
    font-size: 13px;
}

.site-footer__link:hover {
    color: var(--site-primary);
}

.site-footer__bottom {
    display: flex;
    flex-wrap: wrap;
    gap: var(--site-space-3);
    align-items: center;
    justify-content: space-between;
    width: 100%;
    max-width: var(--site-container);
    margin: 0 auto;
    padding: var(--site-space-3) var(--site-gutter);
    border-top: 1px solid var(--site-border);
    box-sizing: border-box;
    font-size: 12px;
    color: var(--site-text-muted);
}

.site-footer__game {
    color: var(--site-text-muted);
}
</style>
