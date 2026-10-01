import type { RouteRecordRaw } from "vue-router";
import type { MenuNode } from "../types/menu";

type ViewLoader = () => Promise<unknown>;

/**
 * 预先登记 src/views 下所有页面，供菜单里的 component 字符串（如 content/board/index）解析。
 * import.meta.glob 会被 Vite 在构建时静态分析，是动态路由的标准做法。
 */
const viewModules: Record<string, ViewLoader> = import.meta.glob("../views/**/*.vue");

const PlaceholderView: ViewLoader = () => import("../views/placeholder/index.vue");

/** 拼接父子路径；子级以 / 开头时视为绝对路径 */
export function joinPath(parentPath: string, path: string): string {
    if (!path) {
        return parentPath;
    }
    if (path.startsWith("/")) {
        return path;
    }
    return `${parentPath.replace(/\/$/, "")}/${path}`;
}

function resolveComponent(component: string | null): ViewLoader {
    if (component) {
        const loader = viewModules[`../views/${component}.vue`];
        if (loader) {
            return loader;
        }
        // 菜单配了组件但页面文件还没写：回退到占位页，避免整棵路由生成失败
        console.warn(`[router] 菜单组件不存在，已回退占位页：${component}`);
    }
    return PlaceholderView;
}

/** 把后端菜单树拍平成挂在 Layout 下的子路由（目录 M 只用于划分层级，不生成路由） */
export function buildRoutes(menus: MenuNode[], parentPath = ""): RouteRecordRaw[] {
    const routes: RouteRecordRaw[] = [];

    for (const menu of menus) {
        const fullPath = joinPath(parentPath, menu.path ?? "");
        const children = menu.children ?? [];

        if (children.length > 0) {
            routes.push(...buildRoutes(children, fullPath));
            continue;
        }
        if (menu.menuType !== "C" || !fullPath) {
            continue;
        }
        routes.push({
            path: fullPath,
            name: `menu-${menu.id}`,
            component: resolveComponent(menu.component),
            meta: { title: menu.menuName, perms: menu.perms, icon: menu.icon },
        } as unknown as RouteRecordRaw);
    }
    return routes;
}

/** 按完整路径在菜单树里找出「从根到目标」的名称链，用于面包屑 */
export function findMenuTrail(menus: MenuNode[], targetPath: string, parentPath = ""): string[] {
    for (const menu of menus) {
        const fullPath = joinPath(parentPath, menu.path ?? "");
        const children = menu.children ?? [];

        if (children.length === 0 && fullPath === targetPath) {
            return [menu.menuName];
        }
        const childTrail = findMenuTrail(children, targetPath, fullPath);
        if (childTrail.length > 0) {
            return [menu.menuName, ...childTrail];
        }
    }
    return [];
}
