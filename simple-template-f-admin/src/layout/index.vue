<template>
    <el-container class="layout">
        <el-aside class="layout__aside" :width="collapsed ? '64px' : '220px'">
            <div class="layout__brand">{{ collapsed ? siteName.slice(0, 1) : siteName }}</div>
            <el-scrollbar class="layout__scroll">
                <el-menu
                    :default-active="route.path"
                    :collapse="collapsed"
                    :collapse-transition="false"
                    background-color="#1f2d3d"
                    text-color="#bfcbd9"
                    active-text-color="#ffd04b"
                    router
                >
                    <SidebarItem
                        v-for="menu in permissionStore.menuTree"
                        :key="menu.id"
                        :menu="menu"
                        base-path=""
                    />
                </el-menu>
            </el-scrollbar>
        </el-aside>

        <el-container class="layout__body">
            <el-header class="layout__header" height="50px">
                <AppHeader v-model:collapsed="collapsed" />
            </el-header>
            <el-main class="layout__main">
                <router-view />
            </el-main>
        </el-container>
    </el-container>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import AppHeader from "./components/AppHeader.vue";
import SidebarItem from "./components/SidebarItem.vue";
import { usePermissionStore } from "../store/permissionStore";
import useSiteStore from "../store/siteStore";

const route = useRoute();
const permissionStore = usePermissionStore();
const siteStore = useSiteStore();
const collapsed = ref(false);

const siteName = computed(() => siteStore.theme?.siteName || "游戏社区");

// 进后台外壳才拉站点配置：登录页不需要主题，且此时带令牌才不会撞 401
onMounted(() => {
    siteStore.load().catch(() => {
        // 失败时保留默认站点名与默认主色，不影响后台使用
    });
});
</script>

<style scoped>
.layout {
    height: 100vh;
}

.layout__aside {
    display: flex;
    flex-direction: column;
    background: #1f2d3d;
    transition: width 0.2s;
}

.layout__brand {
    height: 50px;
    line-height: 50px;
    color: #fff;
    font-weight: 600;
    letter-spacing: 1px;
    text-align: center;
}

.layout__scroll {
    flex: 1;
}

.layout__body {
    background: #f0f2f5;
}

.layout__header {
    padding: 0 16px;
    background: #fff;
    border-bottom: 1px solid #e4e7ed;
}

.layout__main {
    padding: 16px;
}
</style>
