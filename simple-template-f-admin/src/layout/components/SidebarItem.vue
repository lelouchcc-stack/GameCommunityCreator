<template>
    <!-- 有子菜单就渲染成可展开的父项，并在自己内部递归 -->
    <el-sub-menu v-if="children.length > 0" :index="fullPath || `dir-${menu.id}`">
        <template #title>
            <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
            <span>{{ menu.menuName }}</span>
        </template>
        <SidebarItem
            v-for="child in children"
            :key="child.id"
            :menu="child"
            :base-path="fullPath"
        />
    </el-sub-menu>

    <el-menu-item v-else :index="fullPath">
        <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
        <template #title>{{ menu.menuName }}</template>
    </el-menu-item>
</template>

<script lang="ts" setup>
import { computed } from "vue";
import type { MenuNode } from "../../types/menu";
import { joinPath } from "../../router/dynamic";

const props = defineProps<{
    menu: MenuNode;
    basePath: string;
}>();

const children = computed(() => props.menu.children ?? []);
const fullPath = computed(() => joinPath(props.basePath, props.menu.path ?? ""));
</script>
