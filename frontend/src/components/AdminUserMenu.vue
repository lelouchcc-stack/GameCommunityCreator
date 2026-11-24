<script setup lang="ts">
/**
 * 管理端顶部用户菜单：管理端主题切换（深/浅/跟随系统）+ 当前开发者 + 退出登录。
 * 类名 .admin-user-menu__* 保持稳定（自动化测试/E2E 依赖）。
 */
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAdminTheme } from '@/composables/useAdminTheme'
import XIcon from '@/components/admin/XIcon.vue'

const router = useRouter()
const auth = useAuthStore()
const { label, cycleMode, effectiveMode } = useAdminTheme()

async function onLogout(): Promise<void> {
  await auth.logout()
  await router.push({ name: 'auth-login', query: { loggedOut: '1' } })
}
</script>

<template>
  <div class="admin-user-menu">
    <button
      type="button"
      class="admin-user-menu__theme"
      :title="`管理端主题：${label}（点击切换 深色/浅色/跟随系统）`"
      :aria-label="`切换管理端主题，当前${label}`"
      :data-theme-mode="effectiveMode"
      @click="cycleMode()"
    >
      <XIcon :name="effectiveMode === 'light' ? 'sun' : 'moon'" :size="15" />
      <span class="admin-user-menu__theme-label">{{ label }}</span>
    </button>

    <div class="admin-user-menu__divider" aria-hidden="true"></div>

    <span v-if="auth.user" class="admin-user-menu__user" :title="auth.user.username">
      <span class="admin-user-menu__avatar" aria-hidden="true">
        {{ auth.user.displayName.trim().slice(0, 1) || 'G' }}
      </span>
      <span class="admin-user-menu__identity">
        <span class="admin-user-menu__name">{{ auth.user.displayName }}</span>
        <span class="admin-user-menu__handle">@{{ auth.user.username }}</span>
      </span>
    </span>

    <button
      type="button"
      class="admin-user-menu__logout"
      title="退出登录"
      aria-label="退出登录"
      @click="onLogout"
    >
      <XIcon name="logout" :size="15" />
    </button>
  </div>
</template>

<style scoped>
.admin-user-menu {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.admin-user-menu__theme,
.admin-user-menu__logout {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 32px;
  padding: 0 10px;
  border-radius: var(--x-radius-pill);
  border: 1px solid var(--x-border-strong);
  background: transparent;
  color: var(--x-text-secondary);
  font-family: var(--x-mono);
  font-size: 10.5px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    color 0.15s ease,
    background 0.15s ease;
}

.admin-user-menu__theme:hover,
.admin-user-menu__logout:hover {
  color: var(--x-text);
  border-color: #565a60;
  background: var(--gcc-hover);
}

.admin-user-menu__theme-label {
  white-space: nowrap;
}

.admin-user-menu__divider {
  width: 1px;
  height: 18px;
  background: var(--x-border);
}

.admin-user-menu__user {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  max-width: 210px;
}

.admin-user-menu__avatar {
  flex: none;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 1px solid var(--x-border-strong);
  background: var(--x-surface-2);
  color: var(--x-text);
  font-family: var(--x-mono);
  font-size: 11px;
  font-weight: 500;
}

.admin-user-menu__identity {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.25;
}

.admin-user-menu__name {
  font-size: 12px;
  color: var(--x-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-user-menu__handle {
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.06em;
  color: var(--x-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-user-menu__logout {
  padding: 0;
  width: 32px;
}

@media (max-width: 900px) {
  .admin-user-menu__identity,
  .admin-user-menu__theme-label {
    display: none;
  }
}
</style>
