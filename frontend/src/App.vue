<script setup lang="ts">
/**
 * 应用根组件：
 * - /admin/** 使用 AdminShell（侧边栏 + 顶栏 + 主内容区，xAI 控制台风格）
 * - 登录/注册与公开端不套管理端外壳
 * - 管理端主题作用域（.admin-theme-scope[data-admin-theme]）与门户主题完全分离
 */
import { computed, onMounted, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import {
  useAdminTheme,
  initAdminTheme,
  setAdminThemeAttributeEnabled,
} from '@/composables/useAdminTheme'
import { useAuthStore } from '@/stores/auth'
import AdminShell from '@/components/admin/AdminShell.vue'

const route = useRoute()
const auth = useAuthStore()
const { effectiveMode } = useAdminTheme()

initAdminTheme()

const isAdminShell = computed(() => route.path.startsWith('/admin'))
const isAdminArea = computed(
  () =>
    isAdminShell.value || route.name === 'auth-login' || route.name === 'auth-register',
)

// 仅管理端路由把主题属性挂到 <html>（Teleport 内容如 Toast），公开端移除，避免主题串扰
watch(
  [isAdminArea, effectiveMode],
  ([admin]) => {
    setAdminThemeAttributeEnabled(admin)
  },
  { immediate: true },
)

onMounted(() => {
  void auth.init()
})
</script>

<template>
  <div
    class="app-shell"
    :class="{ 'admin-theme-scope': isAdminArea }"
    :data-admin-theme="isAdminArea ? effectiveMode : undefined"
  >
    <AdminShell v-if="isAdminShell" />
    <RouterView v-else />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}
</style>
