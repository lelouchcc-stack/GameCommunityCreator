<template>
    <div class="site">
        <header class="site__header">
            <div class="site__bar">
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
                <!-- 站点外壳里的登录入口：登录页本身是搭建器搭出来的 /login -->
                <div class="site__auth">
                    <template v-if="memberStore.loggedIn">
                        <span class="site__user">
                            {{ memberStore.member?.nickname || memberStore.member?.username }}
                        </span>
                        <button class="site__auth-btn" type="button" @click="onLogout">退出</button>
                    </template>
                    <template v-else>
                        <router-link class="site__auth-btn" to="/login">登录</router-link>
                        <router-link class="site__auth-btn site__auth-btn--ghost" to="/register">注册</router-link>
                    </template>
                </div>
            </div>
            <p v-if="siteStore.theme?.siteIntro" class="site__intro">{{ siteStore.theme.siteIntro }}</p>
        </header>

        <main class="site__main">
            <NodeRenderer v-for="node in nodes" :key="node.id" :node="node" />
            <p v-if="!nodes.length" class="site__empty">
                首页暂无内容，可在后台「站点配置 → 页面搭建」中添加组件
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
import useMemberStore from "../../store/memberStore";
import NodeRenderer from "../../blocks/NodeRenderer.vue";

const siteStore = useSiteStore();
const memberStore = useMemberStore();
const logoFailed = ref(false);

/** 退出即清掉登录态；页头随之切回「登录 / 注册」 */
function onLogout() {
    memberStore.logout();
}

/** 首页组件树：后端已按 visible 过滤并与组件默认值合并，这里直接渲染 */
const nodes = computed(() => siteStore.homePage?.nodes ?? []);

const showLogo = computed(() => Boolean(siteStore.theme?.logo) && !logoFailed.value);

watch(
    () => siteStore.theme?.logo,
    () => {
        logoFailed.value = false;
    }
);
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

.site__bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
}

.site__auth {
    display: flex;
    align-items: center;
    gap: 12px;
}

.site__user {
    font-size: 14px;
    color: var(--site-text-primary);
}

.site__auth-btn {
    padding: 6px 14px;
    border: none;
    border-radius: 6px;
    background-color: var(--site-primary);
    color: #fff;
    font-size: 13px;
    text-decoration: none;
    cursor: pointer;
}

.site__auth-btn--ghost {
    background-color: transparent;
    color: var(--site-text-secondary);
    border: 1px solid var(--site-border);
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
