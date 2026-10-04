<script setup lang="ts">
import { onMounted, provide, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import useSiteStore from "./store/siteStore";
import useMemberStore from "./store/memberStore";
import { BLOCK_RUNTIME_KEY, createBlockRuntime } from "./blocks/blockRuntime";
import { GRAPH_RUNTIME_KEY, createGraphBridge } from "./blocks/graphBridge";
import type { GraphHost } from "./logic/types";
import http from "./utils/request";
import { toast } from "./utils/toast";
import { clearSession, currentMemberId, isLoggedIn, saveSession } from "./utils/session";

const siteStore = useSiteStore();
const memberStore = useMemberStore();
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
    // 登录态统一走 session.ts（唯一事实源）：引擎、localStorage、页面读到的是同一份
    isLoggedIn: () => isLoggedIn(),
    currentMemberId: () => currentMemberId(),
    logout: () => clearSession(),
    // 走图的登录（preset.login 这类组合流）拿到令牌后同样落盘
    onLoginSuccess: (data) => saveSession(data?.token, data?.member),
    onError: (message) => toast(message, "error"),
};

const graphBridge = createGraphBridge(graphHost);

provide(BLOCK_RUNTIME_KEY, blockRuntime);
provide(GRAPH_RUNTIME_KEY, graphBridge);

/**
 * 按当前路由把该页的图装进执行器，再跑一次"进入页面"。
 * 多页下发后图是按页给的：切换路由必须换图，否则会拿上一页的图去跑。
 */
async function syncRouteGraphs() {
    const page = siteStore.pageByPath(route.path);
    graphBridge.setBundle(page?.flows ?? {}, siteStore.presets);
    await graphBridge.triggerEvent("enter");
}

watch(
    () => route.path,
    () => {
        void syncRouteGraphs();
    }
);

onMounted(async () => {
    // 有令牌就先校验并恢复登录态，让"进入页面"的图（如首页 init 演示图）按真实登录态分支
    if (isLoggedIn()) {
        await memberStore.loadMe();
    }
    try {
        // 站点外观 + 全部启用页面（各自节点树与图）+ 被引用的预设，一次拿全
        await siteStore.loadConfig();
        await syncRouteGraphs();
    } catch {
        // 拉取失败时保留缓存或默认主题，页面照常渲染
    }
});
</script>

<template>
    <router-view />
</template>
