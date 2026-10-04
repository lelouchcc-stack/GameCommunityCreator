<script setup lang="ts">
import { onMounted, provide } from "vue";
import { useRoute, useRouter } from "vue-router";
import useSiteStore from "./store/siteStore";
import { BLOCK_RUNTIME_KEY, createBlockRuntime } from "./blocks/blockRuntime";
import { GRAPH_RUNTIME_KEY, createGraphBridge } from "./blocks/graphBridge";
import type { GraphHost } from "./logic/types";
import http from "./utils/request";
import { toast } from "./utils/toast";

const siteStore = useSiteStore();
const router = useRouter();
const route = useRoute();

/** 区块运行时：点击动作（打开弹窗/跳转/滚动/提示）在用户端真正生效 */
const blockRuntime = createBlockRuntime({
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
});

/**
 * 逻辑图的宿主能力：引擎只负责编排，具体怎么发请求/跳转/提示在这里落地。
 * 显隐统一委托给区块运行时，渲染器那边只读一套状态。
 */
const graphHost: GraphHost = {
    async callApi(method, path, params) {
        switch (method) {
            case "GET":
                return http.get<any, any>(path, { params });
            case "PUT":
                return http.put<any, any>(path, params);
            case "DELETE":
                return http.delete<any, any>(path, { params });
            default:
                return http.post<any, any>(path, params);
        }
    },
    navigate(url) {
        if (url === "back") {
            router.back();
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
    scrollTo: (nodeId) => {
        document
            .querySelector(`[data-node-id="${nodeId}"]`)
            ?.scrollIntoView({ behavior: "smooth", block: "center" });
    },
    setVisible: (nodeId, visible) => blockRuntime.setVisible(nodeId as number, visible),
    openModal: (nodeId) => blockRuntime.openModal(nodeId as number),
    closeModal: (nodeId) => blockRuntime.closeModal(nodeId as number),
    // 还没有输入类组件（阶段 4），先返回 undefined；有了之后从组件值注册表读
    componentValue: () => undefined,
    runtimeValues: () => ({
        routeParam: route.params,
        query: route.query,
        page: 1,
    }),
    // 玩家侧登录注册（含用户态）还没做，这里先按令牌判断
    isLoggedIn: () => Boolean(localStorage.getItem("token")),
    currentMemberId: () => null,
    logout: () => {
        localStorage.removeItem("token");
    },
    onError: (message) => toast(message, "error"),
};

const graphBridge = createGraphBridge(graphHost);

provide(BLOCK_RUNTIME_KEY, blockRuntime);
provide(GRAPH_RUNTIME_KEY, graphBridge);

onMounted(async () => {
    try {
        // 站点外观 + 首页组件树 + 首页逻辑图一次拿全
        await siteStore.loadConfig();
        graphBridge.setBundle(siteStore.flows, siteStore.presets);
        // 页面加载跑"进入页面"的图（flowKey=init 的那张就在其中）
        await graphBridge.triggerEvent("enter");
    } catch {
        // 拉取失败时保留缓存或默认主题，页面照常渲染
    }
});
</script>

<template>
    <router-view />
</template>
