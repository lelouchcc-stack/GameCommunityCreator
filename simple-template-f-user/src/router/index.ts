import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import type { PageEntry } from "../types/site";
import useSiteStore from "../store/siteStore";

const routes: RouteRecordRaw[] = [
    {
        /**
         * 兜底路由：所有用户端页面（含首页）都由 PageView 按 ui_page.route_path 渲染。
         * 后台新建页面 + 填好路由即可访问，前端不必为每个页面再加一条路由。
         *
         * 注意：兜底路由的 route.params 里只有 pathMatch，**拿不到 :id**。
         * 带参数的页面（/post/:id、/notice/:id 这类）由 registerPageRoutes 按
         * route_path 真注册成参数路由，组件与逻辑图才能读到 params.id。
         */
        path: "/:pathMatch(.*)*",
        name: "Page",
        component: () => import("../views/PageView.vue"),
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

/** 已经注册过的 routePath，避免每次拉配置都重复 addRoute */
const registeredPaths = new Set<string>();

/**
 * 按后端下发的页面把「带路径参数的页面」注册成真正的参数路由。
 *
 * 为什么需要：兜底路由 /:pathMatch(.*)* 只给一个 pathMatch 数组，
 * route.params.id 是 undefined——帖子详情、公告详情这类页面会拿不到 id。
 * 只挑带 ":" 的路径注册，其余仍走兜底，页面数量增长也不会让路由表膨胀。
 */
function registerPageRoutes(pages: PageEntry[]): void {
    for (const page of pages) {
        const path = page.routePath;
        if (!path || !path.includes(":") || registeredPaths.has(path)) {
            continue;
        }
        registeredPaths.add(path);
        router.addRoute({
            path,
            name: `page-${page.pageKey}`,
            component: () => import("../views/PageView.vue"),
        });
    }
}

/** 页面路由是否已经按站点配置注册过（只做一次） */
let routesReady = false;

/**
 * 首次导航前先把页面路由准备好。
 *
 * 页面路由来自后端下发的 ui_page，必须**在路由匹配之前**注册：
 * 否则 /post/1 会先按兜底路由匹配一次（组件拿不到 :id，先显示"未指定"），
 * 板块页还会先用「主板块」兜底发一次请求、等参数路由补上后再纠正——
 * 既多一次请求，首屏也会闪一下错内容。
 *
 * 拉配置失败时照常放行：兜底路由仍然能渲染页面，只是拿不到路径参数。
 */
router.beforeEach(async (to) => {
    if (routesReady) {
        return true;
    }
    routesReady = true;
    try {
        const siteStore = useSiteStore();
        if (!siteStore.loaded) {
            await siteStore.loadConfig();
        }
        registerPageRoutes(siteStore.pages);
    } catch {
        // 配置拿不到就按兜底路由走，至少能给出页面外壳与 404 提示
    }
    // 路由表刚变过，当前这次导航是按旧表匹配的，重新解析一次才能命中参数路由
    return { path: to.path, query: to.query, hash: to.hash, replace: true };
});

export default router;
