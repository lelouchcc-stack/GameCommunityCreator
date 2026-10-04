<template>
    <div class="page-view">
        <main class="page-view__body">
            <template v-if="page">
                <NodeRenderer v-for="node in page.nodes" :key="node.id" :node="node" />
                <p v-if="!page.nodes.length" class="page-view__empty">
                    该页面还没有内容，可在后台「站点配置 → 页面搭建」中添加组件
                </p>
            </template>

            <div v-else class="page-view__missing">
                <p class="page-view__code">404</p>
                <p class="page-view__text">页面不存在：{{ route.path }}</p>
                <router-link class="page-view__back" to="/">返回首页</router-link>
            </div>
        </main>
    </div>
</template>

<script lang="ts" setup>
/**
 * 通用搭建页：按当前路由地址在 siteStore.pages 里找 routePath 相同的页面并渲染它的组件树。
 * 这是「自定义页路由」的落点 —— 后台新建页面、填好路由即可访问，前端不再为每个页面加路由。
 * 该页逻辑图的装载与"进入页面"触发由 App.vue 按路由统一负责（那里同时管切换路由时换图）。
 */
import { computed } from "vue";
import { useRoute } from "vue-router";
import useSiteStore from "../store/siteStore";
import NodeRenderer from "../blocks/NodeRenderer.vue";

const route = useRoute();
const siteStore = useSiteStore();

const page = computed(() => siteStore.pageByPath(route.path));
</script>

<style scoped>
.page-view {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}

.page-view__body {
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

.page-view__empty {
    color: var(--site-text-secondary, #646a73);
    text-align: center;
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
