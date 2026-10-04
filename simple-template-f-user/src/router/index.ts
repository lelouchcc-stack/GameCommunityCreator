import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";

const routes: RouteRecordRaw[] = [
    {
        path: "/",
        name: "Home",
        component: () => import("../views/home/index.vue"),
        meta: { title: "首页" },
    },
    {
        path: "/board/:id",
        name: "Board",
        component: () => import("../views/board/index.vue"),
        meta: { title: "板块" },
    },
    {
        // 兜底路由：没被上面命中的地址都交给 PageView，按 ui_page.route_path 找页面渲染。
        // 于是后台新建页面 + 填好路由即可访问，前端不必为每个页面再加一条路由。
        path: "/:pathMatch(.*)*",
        name: "Page",
        component: () => import("../views/PageView.vue"),
        meta: { title: "页面" },
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;
