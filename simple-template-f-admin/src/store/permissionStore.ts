import { defineStore } from "pinia";
import { ref } from "vue";
import type { RouteRecordRaw } from "vue-router";
import { getMenuTree } from "../api/menu";
import { buildRoutes } from "../router/dynamic";
import type { MenuNode } from "../types/menu";

/** 菜单树 + 由它生成的动态路由。侧边栏直接读 menuTree，路由由守卫注册 */
export const usePermissionStore = defineStore("permission", () => {
    const menuTree = ref<MenuNode[]>([]);
    const routes = ref<RouteRecordRaw[]>([]);
    const loaded = ref(false);

    async function loadMenus() {
        const menus = await getMenuTree();
        menuTree.value = menus;
        routes.value = buildRoutes(menus);
        loaded.value = true;
        return routes.value;
    }

    function reset() {
        menuTree.value = [];
        routes.value = [];
        loaded.value = false;
    }

    return { menuTree, routes, loaded, loadMenus, reset };
});
