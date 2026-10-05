<template>
    <div class="site">
        <header class="site__header">
            <div class="site__bar">
                <router-link class="site__brand" to="/">
                    <img
                        v-if="showLogo"
                        class="site__logo"
                        :src="siteStore.theme?.logo"
                        alt="站点 Logo"
                        @error="logoFailed = true"
                    />
                    <span class="site__name">{{ siteStore.theme?.siteName || "游戏社区" }}</span>
                </router-link>
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
            <template v-if="page">
                <NodeRenderer v-for="node in page.nodes" :key="node.id" :node="node" />
                <p v-if="!page.nodes.length" class="site__empty">
                    该页面还没有内容，可在后台「站点配置 → 页面搭建」中添加组件
                </p>
            </template>

            <div v-else class="page-view__missing">
                <p class="page-view__code">404</p>
                <p class="page-view__text">页面不存在：{{ route.path }}</p>
                <router-link class="page-view__back" to="/">返回首页</router-link>
            </div>
        </main>

        <footer class="site__footer">
            <p class="site__copyright">{{ siteStore.theme?.copyright }}</p>
        </footer>
    </div>
</template>

<script lang="ts" setup>
/**
 * 通用搭建页：按当前路由地址在 siteStore.pages 里找 routePath 相同的页面并渲染它的组件树
 * （支持 /board/:id、/post/:id 这类参数路由）。这是「自定义页路由」的落点 ——
 * 后台新建页面、填好路由即可访问，前端不再为每个页面加路由。
 * 该页逻辑图的装载与"进入页面"触发由 App.vue 按路由统一负责（那里同时管切换路由时换图）。
 */
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import useSiteStore from "../store/siteStore";
import useMemberStore from "../store/memberStore";
import NodeRenderer from "../blocks/NodeRenderer.vue";

const route = useRoute();
const siteStore = useSiteStore();
const memberStore = useMemberStore();

const page = computed(() => siteStore.pageByPath(route.path));
const logoFailed = ref(false);
const showLogo = computed(() => Boolean(siteStore.theme?.logo) && !logoFailed.value);

function onLogout() {
    memberStore.logout();
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

.site__bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
}

.site__brand {
    display: flex;
    align-items: center;
    gap: 10px;
    color: inherit;
    text-decoration: none;
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

.page-view__missing {
    margin: auto;
    padding: 32px;
    text-align: center;
}

.page-view__code {
    margin: 0;
    font-size: 48px;
    font-weight: 700;
    color: var(--site-text-secondary, #646a73);
}

.page-view__text {
    margin: 8px 0 16px;
    color: var(--site-text-secondary, #646a73);
}

.page-view__back {
    color: var(--site-primary, #3b82f6);
    text-decoration: none;
}
</style>
