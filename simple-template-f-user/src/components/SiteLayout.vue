<template>
    <div class="site-layout">
        <SiteHeader />

        <main class="site-layout__main" :class="{ 'site-layout__main--wide': wide }">
            <slot />
        </main>

        <SiteFooter />
    </div>
</template>

<script lang="ts" setup>
/**
 * 用户端页面外壳：页头 + 版心 + 页脚。
 *
 * 首页与普通搭建页此前各自复制了一份页头/页脚，改一处漏一处；
 * 现在只有这一个外壳，所有页面（含 /login、/register）都挂它，
 * 于是站点名的展示、导航、登录入口、响应式行为天然一致。
 */
import SiteHeader from "./SiteHeader.vue";
import SiteFooter from "./SiteFooter.vue";

withDefaults(
    defineProps<{
        /** 宽版心：首页等需要更宽版面的页面用 */
        wide?: boolean
    }>(),
    { wide: true },
);
</script>

<style scoped>
.site-layout {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
}

.site-layout__main {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: var(--site-space-4);
    width: 100%;
    max-width: var(--site-container-narrow);
    margin: 0 auto;
    padding: var(--site-space-5) var(--site-gutter);
    box-sizing: border-box;
}

.site-layout__main--wide {
    max-width: var(--site-container);
}
</style>
