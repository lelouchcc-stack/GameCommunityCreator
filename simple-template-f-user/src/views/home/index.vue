<template>
    <div class="site">
        <header class="site__header">
            <div class="site__brand">
                <img
                    v-if="showLogo"
                    class="site__logo"
                    :src="siteStore.theme?.logo"
                    alt="站点 Logo"
                    @error="logoFailed = true"
                />
                <span class="site__name">{{ siteStore.theme?.siteName || "游戏社区" }}</span>
            </div>
            <p v-if="siteStore.theme?.siteIntro" class="site__intro">{{ siteStore.theme.siteIntro }}</p>
        </header>

        <main class="site__main">
            <section v-for="module in siteStore.homeModules" :key="module.id" class="module">
                <div class="module__head">
                    <h2 class="module__title">{{ module.moduleName }}</h2>
                    <span class="module__type">{{ module.moduleType }}</span>
                </div>
                <p class="module__note">{{ moduleNote(module) }}</p>
                <p class="module__config">参数：{{ module.configJson || "{}" }}</p>
            </section>

            <p v-if="!siteStore.homeModules.length" class="site__empty">
                首页暂无启用的模块，可在后台「站点配置 → 首页模块」中开启
            </p>
        </main>

        <footer class="site__footer">
            <p class="site__copyright">{{ siteStore.theme?.copyright }}</p>
        </footer>
    </div>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from "vue";
import useSiteStore from "../../store/siteStore";
import type { HomeModule } from "../../types/site";

const siteStore = useSiteStore();
const logoFailed = ref(false);

/** 各模块的内容要等对应业务模块（公告 / 浏览检索 / 板块浏览）接入后填充，这里只体现开关与顺序 */
const MODULE_NOTE: Record<string, string> = {
    banner: "由首页模块配置控制自动播放与间隔",
    notice: "取公告最近 N 条（公告模块接入后填充）",
    hot_post: "按热度取前 N 条帖子（浏览检索模块接入后填充）",
    board_nav: "取开放板块的封面与简介（板块浏览模块接入后填充）",
};

const showLogo = computed(() => Boolean(siteStore.theme?.logo) && !logoFailed.value);

watch(
    () => siteStore.theme?.logo,
    () => {
        logoFailed.value = false;
    }
);

function moduleNote(module: HomeModule) {
    return MODULE_NOTE[module.moduleType] ?? "内容待对应业务模块接入";
}
</script>

<style scoped>
.site {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
}

.site__header {
    padding: 20px 24px;
    background-color: var(--site-header-bg);
    border-bottom: 1px solid var(--site-border);
}

.site__brand {
    display: flex;
    align-items: center;
    gap: 10px;
}

.site__logo {
    width: 32px;
    height: 32px;
    object-fit: contain;
}

.site__name {
    font-size: 20px;
    font-weight: 600;
}

.site__intro {
    margin: 8px 0 0;
    color: var(--site-text-secondary);
    font-size: 14px;
}

.site__main {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 16px;
    width: 100%;
    max-width: 1080px;
    margin: 0 auto;
    padding: 24px;
    box-sizing: border-box;
}

.module {
    padding: 16px;
    background-color: var(--site-bg-card);
    border: 1px solid var(--site-border);
    border-radius: 8px;
}

.module__head {
    display: flex;
    align-items: baseline;
    gap: 8px;
}

.module__title {
    margin: 0;
    font-size: 16px;
    color: var(--site-primary);
}

.module__type {
    color: var(--site-text-secondary);
    font-size: 12px;
}

.module__note {
    margin: 8px 0 0;
    color: var(--site-text-secondary);
    font-size: 14px;
}

.module__config {
    margin: 4px 0 0;
    color: var(--site-text-secondary);
    font-size: 12px;
    font-family: monospace;
}

.site__empty {
    color: var(--site-text-secondary);
    text-align: center;
}

.site__footer {
    padding: 16px 24px;
    border-top: 1px solid var(--site-border);
    text-align: center;
}

.site__copyright {
    margin: 0;
    color: var(--site-text-secondary);
    font-size: 13px;
}
</style>
