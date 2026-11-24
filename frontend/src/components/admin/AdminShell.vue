<script setup lang="ts">
/**
 * 管理端应用壳：左侧 near-black 侧边栏 + 顶部工具栏 + 主内容区。
 *
 * 设计语言：xAI / 工程工具风格
 * - 侧边栏：深黑背景、mono 大写分组标签、active 使用左侧强调线（非大面积高亮）
 * - 顶部栏：搜索框（提交后到项目列表过滤）+ 用户菜单（含管理端主题切换）
 * - 无阴影、无渐变；层级只用 hairline + 背景色差异
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import AdminUserMenu from '@/components/AdminUserMenu.vue'
import XIcon from '@/components/admin/XIcon.vue'

type IconName = 'home' | 'folder' | 'grid' | 'box' | 'file' | 'settings'

interface NavItem {
  name: string
  label: string
  icon: IconName
  match: (path: string) => boolean
}

interface NavGroup {
  label: string
  items: NavItem[]
}

const route = useRoute()
const router = useRouter()

const navGroups: NavGroup[] = [
  {
    label: '工作区',
    items: [
      {
        name: 'admin-overview',
        label: '总览',
        icon: 'home',
        match: (path) => path.startsWith('/admin/overview'),
      },
      {
        name: 'project-list',
        label: '项目',
        icon: 'folder',
        match: (path) =>
          path.startsWith('/admin/projects'),
      },
    ],
  },
  {
    label: '构建',
    items: [
      {
        name: 'admin-templates',
        label: '模板',
        icon: 'grid',
        match: (path) => path.startsWith('/admin/templates'),
      },
      {
        name: 'admin-components',
        label: '组件',
        icon: 'box',
        match: (path) => path.startsWith('/admin/components'),
      },
    ],
  },
  {
    label: '系统',
    items: [
      {
        name: 'admin-settings',
        label: '设置',
        icon: 'settings',
        match: (path) => path.startsWith('/admin/settings'),
      },
    ],
  },
]

const collapsed = ref(false)
const mobileOpen = ref(false)
const isMobile = ref(false)
const keyword = ref('')

const isWorkspace = computed(
  () => route.meta.workspace === true,
)
const currentTitle = computed(() => {
  const title = route.meta.title
  return typeof title === 'string' ? title : 'Game Community Creator'
})

function onResize(): void {
  isMobile.value = window.innerWidth <= 880
  if (!isMobile.value) mobileOpen.value = false
  if (window.innerWidth <= 1280) collapsed.value = true
}

onMounted(() => {
  onResize()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => window.removeEventListener('resize', onResize))

watch(
  () => route.fullPath,
  () => {
    mobileOpen.value = false
  },
)

function isActive(item: NavItem): boolean {
  if (item.name === 'project-list') {
    return (
      route.path.startsWith('/admin/projects') ||
      String(route.name ?? '').startsWith('page-') ||
      route.name === 'site-settings' ||
      route.name === 'content-manager'
    )
  }
  return item.match(route.path)
}

function onSearch(): void {
  const query = keyword.value.trim()
  router.push({ name: 'project-list', query: query ? { q: query } : {} })
  keyword.value = ''
}
</script>

<template>
  <div class="admin-shell" :class="{ 'is-collapsed': collapsed, 'is-mobile-open': mobileOpen }">
    <div v-if="mobileOpen" class="admin-shell__overlay" @click="mobileOpen = false"></div>

    <aside class="admin-shell__sidebar">
      <RouterLink :to="{ name: 'admin-overview' }" class="admin-shell__brand">
        <span class="admin-shell__brand-mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6">
            <path d="M12 2 3 7v10l9 5 9-5V7z" />
            <path d="M12 22V12" />
            <path d="m3 7 9 5 9-5" />
          </svg>
        </span>
        <span class="admin-shell__brand-text">
          <span class="admin-shell__brand-name">GAME COMMUNITY</span>
          <span class="admin-shell__brand-name">CREATOR</span>
        </span>
      </RouterLink>

      <button
        type="button"
        class="admin-shell__collapse"
        :aria-label="collapsed ? '展开侧边栏' : '收起侧边栏'"
        @click="collapsed = !collapsed"
      >
        <XIcon :name="collapsed ? 'menu' : 'chevron-down'" :size="13" />
      </button>

      <nav class="admin-shell__nav" aria-label="管理端导航">
        <div v-for="group in navGroups" :key="group.label" class="admin-shell__nav-group">
          <p class="admin-shell__nav-label">{{ group.label }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="item.name"
            :to="{ name: item.name }"
            class="admin-shell__nav-item"
            :class="{ 'is-active': isActive(item) }"
            :title="item.label"
          >
            <span class="admin-shell__nav-indicator" aria-hidden="true"></span>
            <XIcon :name="item.icon" :size="16" />
            <span class="admin-shell__nav-text">{{ item.label }}</span>
          </RouterLink>
        </div>
      </nav>

      <div class="admin-shell__sidebar-foot">
        <p class="admin-shell__version x-mono">GCC / v3.0 · LOCAL</p>
      </div>
    </aside>

    <div class="admin-shell__main">
      <header class="admin-shell__topbar">
        <button
          type="button"
          class="admin-shell__mobile-toggle"
          aria-label="打开导航"
          @click="mobileOpen = !mobileOpen"
        >
          <XIcon name="menu" :size="17" />
        </button>

        <form class="admin-shell__search" role="search" @submit.prevent="onSearch">
          <XIcon name="search" :size="15" class="admin-shell__search-icon" />
          <input
            v-model="keyword"
            class="admin-shell__search-input"
            type="search"
            placeholder="搜索项目、模板或功能…"
            aria-label="搜索项目"
          />
          <span class="admin-shell__search-hint x-mono">⌘K</span>
        </form>

        <span class="admin-shell__context x-mono">{{ currentTitle }}</span>

        <div class="admin-shell__topbar-actions">
          <AdminUserMenu />
        </div>
      </header>

      <main class="admin-shell__content" :class="{ 'is-workspace': isWorkspace }">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  --admin-sidebar-width: 232px;
  display: flex;
  min-height: 100vh;
  background: var(--x-canvas);
  color: var(--x-text);
}

.admin-theme-scope[data-admin-theme='light'] .admin-shell {
  background: var(--gcc-bg);
}

.admin-shell__overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  z-index: 40;
}

/* ---------------- sidebar ---------------- */
.admin-shell__sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  width: var(--admin-sidebar-width);
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px 14px 16px;
  border-right: 1px solid var(--x-border);
  background: #0b0b0c;
  z-index: 50;
}

.admin-theme-scope[data-admin-theme='light'] .admin-shell__sidebar {
  background: #ffffff;
  border-right-color: var(--gcc-border);
}

.admin-shell__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 6px 18px;
  color: var(--x-text);
  border-bottom: 1px solid var(--x-border);
  margin-bottom: 6px;
}

.admin-theme-scope[data-admin-theme='light'] .admin-shell__brand {
  color: var(--gcc-text);
  border-bottom-color: var(--gcc-border);
}

.admin-shell__brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex: none;
  color: var(--x-text);
}

.admin-shell__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  min-width: 0;
}

.admin-shell__brand-name {
  font-family: var(--x-mono);
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.14em;
  white-space: nowrap;
}

.admin-shell__collapse {
  position: absolute;
  right: 10px;
  top: 26px;
  width: 22px;
  height: 22px;
  display: none;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--x-border);
  border-radius: 50%;
  background: var(--x-surface);
  color: var(--x-muted);
  cursor: pointer;
}

.admin-shell__nav {
  display: flex;
  flex-direction: column;
  gap: 18px;
  overflow-y: auto;
  flex: 1;
  padding-top: 8px;
}

.admin-shell__nav-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.admin-shell__nav-label {
  padding: 0 10px 6px;
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--x-muted);
  opacity: 0.72;
}

.admin-shell__nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  color: var(--x-muted);
  font-size: 12.5px;
  letter-spacing: 0.02em;
  transition:
    color 0.15s ease,
    background 0.15s ease;
}

.admin-shell__nav-item:hover {
  color: var(--x-text);
  background: var(--x-surface-2);
}

.admin-theme-scope[data-admin-theme='light'] .admin-shell__nav-item:hover {
  color: var(--gcc-text);
  background: var(--gcc-hover);
}

.admin-shell__nav-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%) scaleY(0);
  width: 2px;
  height: 16px;
  border-radius: 2px;
  background: var(--x-accent);
  transition: transform 0.15s ease;
}

.admin-shell__nav-item.is-active {
  color: var(--x-text);
  background: transparent;
}

.admin-shell__nav-item.is-active .admin-shell__nav-indicator {
  transform: translateY(-50%) scaleY(1);
}

.admin-theme-scope[data-admin-theme='light'] .admin-shell__nav-item.is-active {
  color: var(--gcc-text);
}

.admin-shell__nav-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-shell__sidebar-foot {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--x-border);
}

.admin-theme-scope[data-admin-theme='light'] .admin-shell__sidebar-foot {
  border-top-color: var(--gcc-border);
}

.admin-shell__community {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 12px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
  color: var(--x-text);
  transition: border-color 0.15s ease;
}

.admin-theme-scope[data-admin-theme='light'] .admin-shell__community {
  background: var(--gcc-surface);
  border-color: var(--gcc-border);
  color: var(--gcc-text);
}

.admin-shell__community:hover {
  border-color: var(--x-border-strong);
}

.admin-shell__community-kicker {
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.16em;
  color: var(--x-muted);
}

.admin-shell__community-title {
  font-size: 12.5px;
}

.admin-shell__community-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  font-family: var(--x-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--x-text-secondary);
}

.admin-shell__version {
  font-size: 9px;
  letter-spacing: 0.12em;
  color: var(--x-muted);
  opacity: 0.6;
  padding-left: 2px;
}

/* ---------------- main / topbar ---------------- */
.admin-shell__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-shell__topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 16px;
  height: 64px;
  padding: 0 28px;
  border-bottom: 1px solid var(--x-border);
  background: var(--gcc-header-bg);
  backdrop-filter: blur(10px);
}

.admin-theme-scope[data-admin-theme='light'] .admin-shell__topbar {
  border-bottom-color: var(--gcc-border);
}

.admin-shell__mobile-toggle {
  display: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--x-border-strong);
  border-radius: 50%;
  background: transparent;
  color: var(--x-text);
  cursor: pointer;
}

.admin-shell__search {
  display: flex;
  align-items: center;
  gap: 9px;
  width: min(420px, 38vw);
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-pill);
  background: var(--x-surface);
  transition: border-color 0.15s ease;
}

.admin-theme-scope[data-admin-theme='light'] .admin-shell__search {
  background: var(--gcc-surface);
  border-color: var(--gcc-border);
}

.admin-shell__search:focus-within {
  border-color: var(--x-border-strong);
}

.admin-shell__search-icon {
  color: var(--x-muted);
}

.admin-shell__search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  color: var(--x-text);
  font-size: 12.5px;
}

.admin-theme-scope[data-admin-theme='light'] .admin-shell__search-input {
  color: var(--gcc-text);
}

.admin-shell__search-input::placeholder {
  color: var(--x-muted);
}

.admin-shell__search-input::-webkit-search-cancel-button {
  display: none;
}

.admin-shell__search-hint {
  font-size: 9.5px;
  letter-spacing: 0.06em;
  color: var(--x-muted);
  padding: 2px 6px;
  border: 1px solid var(--x-border);
  border-radius: 4px;
  opacity: 0.8;
}

.admin-shell__context {
  display: none;
  font-size: 9.5px;
  letter-spacing: 0.14em;
  color: var(--x-muted);
}

.admin-shell__topbar-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-shell__content {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
}

.admin-shell__content :deep(> *) {
  min-height: 100%;
}

.admin-shell__content.is-workspace {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  height: calc(100vh - 64px);
  overflow: hidden;
}

/* ---------------- responsive ---------------- */
@media (max-width: 1180px) {
  .admin-shell__context {
    display: inline;
  }
}

@media (max-width: 880px) {
  .admin-shell__sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
  }

  .admin-shell.is-mobile-open .admin-shell__sidebar {
    transform: translateX(0);
  }

  .admin-shell__mobile-toggle {
    display: inline-flex;
  }

  .admin-shell__search {
    width: min(320px, 44vw);
  }

  .admin-shell__topbar {
    padding: 0 16px;
    gap: 10px;
  }
}

@media (max-width: 620px) {
  .admin-shell__search {
    display: none;
  }
}
</style>
