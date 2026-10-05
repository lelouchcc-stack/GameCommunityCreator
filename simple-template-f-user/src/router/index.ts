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
        // 兜底路由：/board/:id、/post/:id、/post/list 等都由 PageView 按 ui_page.route_path 找页面渲染
        // （支持 :参数 匹配）。于是后台新建页面 + 填好路由即可访问，前端不必为每个页面再加一条路由。
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
