<template>
    <el-card shadow="never">
        <el-empty :description="`「${title}」页面还没实现`">
            <p class="placeholder__hint">
                菜单与路由已经由后台 <code>sys_menu</code> 驱动，这个模块的开发页面还没编写。
            </p>
        </el-empty>
    </el-card>
</template>

<script lang="ts" setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();

const title = computed(() => {
    const routeTitle = route.meta.title;
    return typeof routeTitle === "string" ? routeTitle : "该";
});
</script>

<style scoped>
.placeholder__hint {
    margin: 0;
    color: #909399;
    font-size: 13px;
}
</style>
