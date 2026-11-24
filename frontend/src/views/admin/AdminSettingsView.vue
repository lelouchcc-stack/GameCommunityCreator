<script setup lang="ts">
/**
 * 管理端设置（/admin/settings）—— SETTINGS 页面。
 * 账号信息、管理端主题、外观说明与关于信息；不修改业务配置（站点主题在站点设置里）。
 */
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/admin/PageHeader.vue'
import XIcon from '@/components/admin/XIcon.vue'
import { useAdminTheme, ADMIN_THEME_LABELS } from '@/composables/useAdminTheme'
import type { ThemeMode } from '@/types/site'

const router = useRouter()
const auth = useAuthStore()
const { mode, setMode } = useAdminTheme()

const themeOptions: ThemeMode[] = ['DARK', 'LIGHT', 'SYSTEM']

async function onLogout(): Promise<void> {
  await auth.logout()
  await router.push({ name: 'auth-login', query: { loggedOut: '1' } })
}
</script>

<template>
  <div class="settings-page x-page gcc-container">
    <PageHeader kicker="Settings" title="设置">
      <template #actions>
        <RouterLink class="gcc-btn gcc-btn-sm gcc-btn-ghost" :to="{ name: 'admin-overview' }">
          返回总览
        </RouterLink>
      </template>
    </PageHeader>

    <div class="settings-layout">
      <section class="settings-card">
        <header class="settings-card__head">
          <p class="x-kicker">Account</p>
          <h2 class="settings-card__title">开发者账号</h2>
        </header>
        <dl class="settings-list">
          <div class="settings-row">
            <dt>显示名称</dt>
            <dd>{{ auth.user?.displayName ?? '—' }}</dd>
          </div>
          <div class="settings-row">
            <dt>用户名</dt>
            <dd class="x-mono">@{{ auth.user?.username ?? '—' }}</dd>
          </div>
          <div class="settings-row">
            <dt>账号 ID</dt>
            <dd class="x-mono">#{{ auth.user?.id ?? '—' }}</dd>
          </div>
        </dl>
        <footer class="settings-card__foot">
          <button type="button" class="gcc-btn gcc-btn-danger" @click="onLogout">
            <XIcon name="logout" :size="13" />
            退出登录
          </button>
        </footer>
      </section>

      <section class="settings-card">
        <header class="settings-card__head">
          <p class="x-kicker">Appearance</p>
          <h2 class="settings-card__title">管理端主题</h2>
        </header>
        <div class="settings-themes" role="radiogroup" aria-label="管理端主题">
          <button
            v-for="option in themeOptions"
            :key="option"
            type="button"
            role="radio"
            :aria-checked="mode === option"
            class="settings-theme"
            :class="{ 'is-active': mode === option }"
            @click="setMode(option)"
          >
            <span class="settings-theme__head">
              <span class="settings-theme__name">{{ ADMIN_THEME_LABELS[option] }}</span>
              <span v-if="mode === option" class="settings-theme__check">
                <XIcon name="check" :size="13" />
              </span>
            </span>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  padding-top: 36px;
}

.settings-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.settings-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
}

.settings-card--muted {
  background: var(--x-canvas-soft);
}

.settings-card__head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.settings-card__title {
  margin: 0;
  font-size: 17px;
  font-weight: 500;
  letter-spacing: -0.01em;
}

.settings-card__desc {
  color: var(--x-muted);
  font-size: 12px;
  line-height: 1.7;
}

.settings-card__hint {
  font-size: 8.5px;
  letter-spacing: 0.12em;
  color: var(--x-muted);
  opacity: 0.75;
}

.settings-card__foot {
  display: flex;
  gap: 10px;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--x-border);
}

.settings-list {
  display: flex;
  flex-direction: column;
  margin: 0;
}

.settings-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20px;
  padding: 10px 0;
  border-top: 1px solid var(--x-border);
}

.settings-row:first-child {
  border-top: none;
  padding-top: 0;
}

.settings-row dt {
  flex: none;
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.settings-row dd {
  margin: 0;
  text-align: right;
  font-size: 12.5px;
  color: var(--x-text-secondary);
}

.settings-themes {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.settings-theme {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: transparent;
  color: var(--x-text);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease;
}

.settings-theme:hover {
  border-color: var(--x-border-strong);
}

.settings-theme.is-active {
  border-color: var(--x-accent);
}

.settings-theme__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.settings-theme__name {
  font-size: 13px;
}

.settings-theme__check {
  color: var(--x-accent);
}

.settings-theme__desc {
  color: var(--x-muted);
  font-size: 11.5px;
}

.settings-link {
  color: var(--x-text-secondary);
  text-decoration: underline;
  text-underline-offset: 3px;
}

@media (max-width: 1024px) {
  .settings-layout {
    grid-template-columns: 1fr;
  }
}
</style>
