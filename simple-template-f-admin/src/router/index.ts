import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";

/**
 * 静态路由：登录页 + 后台外壳。
 * 业务页面由 router/guard.ts 在登录后按菜单动态注册到名为 Layout 的路由下。
 */
export const constantRoutes: RouteRecordRaw[] = [
    {
        path: "/login",
        name: "Login",
        component: () => import("../views/login.vue"),
    },
    {
        path: "/",
        name: "Layout",
        component: () => import("../layout/index.vue"),
        redirect: "/dashboard",
        children: [
            {
                path: "dashboard",
                name: "Dashboard",
                component: () => import("../views/dashboard/index.vue"),
                meta: { title: "工作台", icon: "HomeFilled" },
            },
        ],
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes: constantRoutes,
});

export default router;
