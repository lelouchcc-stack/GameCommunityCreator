<template>
    <div class="header">
        <div class="header__left">
            <el-icon class="header__toggle" @click="toggle">
                <component :is="collapsed ? 'Expand' : 'Fold'" />
            </el-icon>
            <el-breadcrumb separator="/">
                <el-breadcrumb-item v-for="(item, index) in breadcrumb" :key="index">
                    {{ item }}
                </el-breadcrumb-item>
            </el-breadcrumb>
        </div>

        <el-dropdown @command="onCommand">
            <span class="header__user">
                {{ displayName }}
                <el-icon class="header__caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
                <el-dropdown-menu>
                    <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
            </template>
        </el-dropdown>
    </div>
</template>

<script lang="ts" setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowDown } from "@element-plus/icons-vue";
import useUserStore from "../../store/userStore";
import { usePermissionStore } from "../../store/permissionStore";
import { findMenuTrail } from "../../router/dynamic";

const props = defineProps<{ collapsed: boolean }>();
const emit = defineEmits<{ "update:collapsed": [value: boolean] }>();

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const permissionStore = usePermissionStore();

const displayName = computed(
    () => userStore.userInfo?.nickname || userStore.userInfo?.username || "未登录"
);

/** 面包屑优先按菜单树还原层级（如 内容管理 / 板块管理），找不到就退回当前路由标题 */
const breadcrumb = computed(() => {
    const trail = findMenuTrail(permissionStore.menuTree, route.path);
    if (trail.length > 0) {
        return trail;
    }
    const { title } = route.meta;
    return typeof title === "string" ? [title] : [];
});

function toggle() {
    emit("update:collapsed", !props.collapsed);
}

async function onCommand(command: string) {
    if (command !== "logout") {
        return;
    }
    // 动态路由是登录后才注册的，退出时必须摘掉，否则换账号登录会看到上一个人的菜单
    permissionStore.routes.forEach((item) => {
        if (item.name) {
            router.removeRoute(item.name);
        }
    });
    userStore.clear();
    permissionStore.reset();
    ElMessage.success("已退出登录");
    await router.replace("/login");
}
</script>

<style scoped>
.header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 100%;
}

.header__left {
    display: flex;
    align-items: center;
    gap: 16px;
}

.header__toggle {
    cursor: pointer;
    font-size: 18px;
}

.header__user {
    display: flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    outline: none;
}

.header__caret {
    font-size: 12px;
}
</style>
