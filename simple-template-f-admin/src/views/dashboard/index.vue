<template>
    <div class="dashboard">
        <el-card shadow="never">
            <h2 class="dashboard__title">工作台</h2>
            <p class="dashboard__line">
                欢迎回来，{{ displayName }}
                <el-tag v-if="roles.length > 0" size="small">{{ roles.join(" / ") }}</el-tag>
            </p>
            <p class="dashboard__hint">
                左侧菜单由后台 <code>sys_menu</code> 表驱动，当前账号可见
                <b>{{ leafCount }}</b> 个功能入口。
            </p>
        </el-card>

        <el-card class="dashboard__next" shadow="never">
            <template #header>下一步</template>
            <p class="dashboard__hint">
                登录、菜单、动态路由、权限这道链路已经打通；各业务模块的开发页面还没编写，
                点击左侧任意菜单会进入占位页。
            </p>
        </el-card>
    </div>
</template>

<script lang="ts" setup>
import { computed } from "vue";
import useUserStore from "../../store/userStore";
import { usePermissionStore } from "../../store/permissionStore";
import type { MenuNode } from "../../types/menu";

const userStore = useUserStore();
const permissionStore = usePermissionStore();

const displayName = computed(
    () => userStore.userInfo?.nickname || userStore.userInfo?.username || "未知用户"
);
const roles = computed(() => userStore.userInfo?.roles ?? []);

function countLeaves(menus: MenuNode[]): number {
    return menus.reduce((total, menu) => {
        const children = menu.children ?? [];
        return total + (children.length > 0 ? countLeaves(children) : 1);
    }, 0);
}

const leafCount = computed(() => countLeaves(permissionStore.menuTree));
</script>

<style scoped>
.dashboard__title {
    margin: 0 0 12px;
    font-size: 18px;
}

.dashboard__line {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 8px;
}

.dashboard__hint {
    margin: 0;
    color: #909399;
    font-size: 13px;
    line-height: 1.8;
}

.dashboard__next {
    margin-top: 16px;
}
</style>
