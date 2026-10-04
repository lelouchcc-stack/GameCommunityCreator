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
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;
