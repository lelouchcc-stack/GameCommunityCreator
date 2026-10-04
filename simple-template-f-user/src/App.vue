<script setup lang="ts">
import { onMounted, provide } from "vue";
import { useRouter } from "vue-router";
import useSiteStore from "./store/siteStore";
import { BLOCK_RUNTIME_KEY, createBlockRuntime } from "./blocks/blockRuntime";
import { toast } from "./utils/toast";

const siteStore = useSiteStore();
const router = useRouter();

/** 区块运行时：点击动作（打开弹窗/跳转/滚动/提示）在用户端真正生效 */
provide(
    BLOCK_RUNTIME_KEY,
    createBlockRuntime({
        navigate: (url, newTab) => {
            if (newTab) {
                window.open(url);
                return;
            }
            if (/^https?:\/\//.test(url)) {
                window.location.href = url;
                return;
            }
            router.push(url).catch(() => {
                window.location.href = url;
            });
        },
        toast,
        scrollTo: (id) => {
            document
                .querySelector(`[data-node-id="${id}"]`)
                ?.scrollIntoView({ behavior: "smooth", block: "center" });
        },
    }),
);

onMounted(() => {
    // 站点外观配置在这里拉一次：主题即时生效，首页组件树供 HomeView 渲染
    siteStore.loadConfig().catch(() => {
        // 拉取失败时保留缓存或默认主题，页面照常渲染
    });
});
</script>

<template>
    <router-view />
</template>
