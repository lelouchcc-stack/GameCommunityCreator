<script setup lang="ts">
import { onMounted } from "vue";
import useSiteStore from "./store/siteStore";

const siteStore = useSiteStore();

// 站点外观配置在这里拉一次：主题即时生效，首页模块列表供 HomeView 渲染
onMounted(() => {
    siteStore.loadConfig().catch(() => {
        // 拉取失败时保留缓存或默认主题，页面照常渲染
    });
});
</script>

<template>
    <router-view />
</template>
