import type { Router } from "vue-router";
import { getMe } from "../api/user";
import useUserStore from "../store/userStore";
import { usePermissionStore } from "../store/permissionStore";

/** 未登录也能访问的路径 */
const whiteList = ["/login"];

export function setupRouterGuard(router: Router) {
    router.beforeEach(async (to) => {
        const userStore = useUserStore();

        // 未登录：放行白名单，其余跳登录页并记住来源
        if (!userStore.token) {
            if (whiteList.includes(to.path)) {
                return true;
            }
            return { path: "/login", query: { redirect: to.fullPath } };
        }

        // 已登录就别再回登录页
        if (to.path === "/login") {
            return "/";
        }

        const permissionStore = usePermissionStore();
        if (permissionStore.loaded) {
            return true;
        }

        try {
            // 刷新页面后 store 是空的，要用令牌把账号信息和菜单重新取回来
            if (!userStore.userInfo) {
                userStore.setUserInfo(await getMe());
            }
            const dynamicRoutes = await permissionStore.loadMenus();
            dynamicRoutes.forEach((item) => router.addRoute("Layout", item));
            // 路由表刚变了，重新走一次当前地址，否则这次导航匹配不到刚注册的路由
            return { ...to, replace: true };
        } catch {
            userStore.clear();
            permissionStore.reset();
            return { path: "/login", query: { redirect: to.fullPath } };
        }
    });
}
