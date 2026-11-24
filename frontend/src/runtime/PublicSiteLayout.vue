<script setup lang="ts">
/**
 * 公开站点布局：xAI 风格游戏门户外壳（近黑 header + mono 大写导航 + 主题切换 + 极简页脚）。
 *
 * - 导航来自公开接口下发的 navigation（后端已过滤未发布目标；前端再做一次防御过滤）；
 * - 主题切换：深色 → 浅色 → 跟随系统，useSiteTheme 记忆到 localStorage；
 *   最终模式同时通过 data-theme 与 inject 传给 CommunityRuntime；
 * - 页面品牌色来自当前页面 theme，与站点明暗模式合并成一套 CSS 变量（公开端不受管理端主题影响）；
 * - **页脚唯一（BUGFIX_PLAN §1）**：`showFooter` 为 false 时不渲染布局页脚，
 *   由页面配置里的 footer 组件负责（布局同时把已解析的页脚导航 inject 给该组件）。
 */
import { computed, onMounted, provide, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useSiteTheme } from '@/composables/useSiteTheme'
import XIcon from '@/components/admin/XIcon.vue'
import { DEFAULT_THEME, type ThemeConfig } from '@/types/page'
import type {
  NavigationItemView,
  NavigationLocation,
  PublicSiteDetail,
} from '@/types/site'
import { isValidHttpUrl, pathForPage } from '@/utils/actions'
import { THEME_MODE_LABELS } from '@/utils/theme'
import { RUNTIME_FOOTER_NAV_KEY, type RuntimeNavItem } from './context'
import { ensureSystemThemeListener, buildThemeStyle, RUNTIME_THEME_KEY } from './themeContext'

const props = withDefaults(
  defineProps<{
    detail: PublicSiteDetail
    /** 当前页面主题（品牌色）；缺省用默认主题 */
    pageTheme?: ThemeConfig | null
    /** 是否渲染布局页脚（页面配置已含 footer 组件时传 false，保证全页只有一个页脚） */
    showFooter?: boolean
  }>(),
  { pageTheme: null, showFooter: true },
)

const route = useRoute()

const siteTheme = useSiteTheme(
  props.detail.project.slug,
  () => props.detail.settings?.themeMode ?? 'DARK',
)

provide(RUNTIME_THEME_KEY, siteTheme.effectiveMode)

onMounted(() => ensureSystemThemeListener())

const effectiveMode = computed(() => siteTheme.effectiveMode.value)
const themeSelection = computed(() => siteTheme.userMode.value ?? siteTheme.siteMode.value)
const themeLabel = computed(() => THEME_MODE_LABELS[themeSelection.value])
const themeIconName = computed<'sun' | 'moon' | 'monitor'>(() => {
  if (themeSelection.value === 'LIGHT') return 'sun'
  if (themeSelection.value === 'DARK') return 'moon'
  return 'monitor'
})
const themeTitle = computed(() => `切换主题（当前：${themeLabel.value}）`)

const menuOpen = ref(false)
watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  },
)

const project = computed(() => props.detail.project)
const homePath = computed(() => `/community/${project.value.slug}`)
const brandInitial = computed(() =>
  (project.value.gameName || project.value.name || 'G').trim().slice(0, 1).toUpperCase(),
)

const rootStyle = computed<Record<string, string>>(() =>
  buildThemeStyle(props.pageTheme ?? DEFAULT_THEME, effectiveMode.value),
)

function isCurrentPath(path: string): boolean {
  const current = route.path
  if (path === homePath.value) return current === path
  return current === path || current.startsWith(`${path}/`)
}

function pagePathFor(targetPageId: string | null): string | null {
  if (!targetPageId) return null
  const pages = Array.isArray(props.detail.pages) ? props.detail.pages : []
  const page = pages.find((item) => item.pageId === targetPageId)
  if (!page) return null
  return pathForPage(page, props.detail.project.slug)
}

/** 过滤不可用导航：page 目标必须存在且已发布（detail.pages 只含已发布页），external 必须 http(s) */
function resolveItems(location: NavigationLocation): RuntimeNavItem[] {
  const items: NavigationItemView[] = Array.isArray(props.detail.navigation)
    ? props.detail.navigation
    : []
  return items
    .filter((item) => item.location === location && item.visible !== false)
    .slice()
    .sort((left, right) => left.sortOrder - right.sortOrder || left.id - right.id)
    .flatMap<RuntimeNavItem>((item) => {
      if (item.targetType === 'external') {
        if (!isValidHttpUrl(item.url)) return []
        return [
          {
            key: `nav-${item.id}`,
            title: item.title,
            path: item.url.trim(),
            external: true,
            current: false,
          },
        ]
      }
      const path = pagePathFor(item.targetPageId)
      if (!path) return []
      return [
        {
          key: `nav-${item.id}`,
          title: item.title,
          path,
          external: false,
          current: isCurrentPath(path),
        },
      ]
    })
}

const headerItems = computed(() => resolveItems('header'))
const footerItems = computed(() => resolveItems('footer'))
const currentYear = new Date().getFullYear()

// 布局页脚隐藏时（页面配置含 footer 组件），把同一份页脚导航交给 FooterComponent，
// 保证页脚导航不因「页脚唯一」而丢失。
provide(RUNTIME_FOOTER_NAV_KEY, footerItems)
</script>

<template>
  <div
    class="site-layout"
    :style="rootStyle"
    :data-theme="effectiveMode"
    :data-site-slug="project.slug"
  >
    <header class="site-header">
      <div class="site-header__inner gcc-container">
        <RouterLink class="site-brand" :to="homePath">
          <span class="site-brand__mark" aria-hidden="true">{{ brandInitial }}</span>
          <span class="site-brand__text">
            <strong class="site-brand__game">{{ project.gameName || project.name }}</strong>
            <span class="site-brand__site">{{ project.name }}</span>
          </span>
        </RouterLink>

        <nav
          v-if="headerItems.length"
          class="site-nav"
          :class="{ 'site-nav--open': menuOpen }"
          aria-label="站内导航"
        >
          <template v-for="item in headerItems" :key="item.key">
            <a
              v-if="item.external"
              class="site-nav__link"
              :href="item.path"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ item.title }}<span class="site-nav__arrow" aria-hidden="true">↗</span>
            </a>
            <RouterLink
              v-else
              class="site-nav__link"
              :class="{ 'site-nav__link--current': item.current }"
              :to="item.path"
              :aria-current="item.current ? 'page' : undefined"
            >
              {{ item.title }}
            </RouterLink>
          </template>
        </nav>

        <div class="site-header__actions">
          <button
            type="button"
            class="theme-toggle"
            :title="themeTitle"
            :aria-label="themeTitle"
            @click="siteTheme.cycleMode()"
          >
            <XIcon :name="themeIconName" :size="14" />
            <span class="theme-toggle__label">{{ themeLabel }}</span>
          </button>
          <RouterLink class="site-header__gallery" to="/community">
            社区广场
            <XIcon name="arrow-up-right" :size="12" />
          </RouterLink>
          <button
            type="button"
            class="site-header__menu"
            :aria-label="menuOpen ? '收起导航菜单' : '打开导航菜单'"
            :aria-expanded="menuOpen"
            @click="menuOpen = !menuOpen"
          >
            <XIcon :name="menuOpen ? 'close' : 'menu'" :size="17" />
          </button>
        </div>
      </div>
    </header>

    <main class="site-main">
      <slot />
    </main>

    <footer v-if="showFooter" class="site-footer" data-layout-footer>
      <div class="site-footer__inner gcc-container">
        <div class="site-footer__main">
          <div class="site-footer__brand">
            <span class="site-footer__mark" aria-hidden="true">{{ brandInitial }}</span>
            <div class="site-footer__brand-text">
              <p class="site-footer__game">{{ project.gameName || project.name }}</p>
              <p class="site-footer__meta">© {{ currentYear }} {{ project.name }}</p>
            </div>
          </div>
          <nav v-if="footerItems.length" class="site-footer__nav" aria-label="页脚导航">
            <template v-for="item in footerItems" :key="`footer-${item.key}`">
              <a
                v-if="item.external"
                class="site-footer__link"
                :href="item.path"
                target="_blank"
                rel="noopener noreferrer"
              >
                {{ item.title }}<span aria-hidden="true">↗</span>
              </a>
              <RouterLink v-else class="site-footer__link" :to="item.path">
                {{ item.title }}
              </RouterLink>
            </template>
          </nav>
        </div>
        <p class="site-footer__built">BUILT WITH GAME COMMUNITY CREATOR</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.site-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  color: var(--gcc-text);
  font-family: var(--gcc-font);
}

/* ---------------- header ---------------- */
.site-header {
  position: sticky;
  top: 0;
  z-index: 40;
  border-bottom: 1px solid var(--gcc-border);
  background: var(--gcc-header-bg);
  backdrop-filter: blur(10px);
}

.site-header__inner {
  display: flex;
  align-items: center;
  gap: 28px;
  height: 64px;
}

.site-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: none;
  min-width: 0;
  color: var(--gcc-text);
}

.site-brand__mark {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 6px;
  border: 1px solid var(--gcc-border-strong);
  color: var(--gcc-text);
  font-family: var(--gcc-mono);
  font-size: 12px;
  font-weight: 500;
}

.site-brand__text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.site-brand__game {
  font-size: 13.5px;
  font-weight: 500;
  line-height: 1.3;
  letter-spacing: -0.01em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.site-brand__site {
  font-family: var(--gcc-mono);
  font-size: 9px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 240px;
}

.site-nav {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 2px;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}

.site-nav::-webkit-scrollbar {
  display: none;
}

.site-nav__link {
  position: relative;
  flex: none;
  padding: 7px 11px;
  border-bottom: 1px solid transparent;
  color: var(--gcc-muted);
  font-family: var(--gcc-mono);
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  line-height: 1.4;
  white-space: nowrap;
  transition:
    color 0.15s ease,
    border-color 0.15s ease;
}

.site-nav__link:hover {
  color: var(--gcc-text);
}

.site-nav__link--current {
  color: var(--gcc-text);
  border-bottom-color: var(--gcc-accent);
}

.site-nav__arrow {
  margin-left: 3px;
  font-size: 10px;
  opacity: 0.8;
}

.site-header__actions {
  flex: none;
  display: flex;
  align-items: center;
  gap: 10px;
}

.theme-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 9999px;
  border: 1px solid var(--gcc-border-strong);
  background: transparent;
  color: var(--gcc-muted);
  font-size: 11.5px;
  line-height: 1.4;
  cursor: pointer;
  transition:
    color 0.15s ease,
    border-color 0.15s ease,
    background 0.15s ease;
}

.theme-toggle:hover {
  color: var(--gcc-text);
  border-color: var(--gcc-text);
  background: var(--gcc-hover);
}

.theme-toggle__label {
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.site-header__gallery {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  white-space: nowrap;
  transition: color 0.15s ease;
}

.site-header__gallery:hover {
  color: var(--gcc-text);
}

.site-header__menu {
  display: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--gcc-border-strong);
  background: transparent;
  color: var(--gcc-text);
  cursor: pointer;
}

/* ---------------- content ---------------- */
.site-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.site-main :deep(.gcc-runtime) {
  flex: 1;
}

/* ---------------- footer ---------------- */
.site-footer {
  margin-top: auto;
  border-top: 1px solid var(--gcc-border);
  background: transparent;
}

.site-footer__inner {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 40px 0 32px;
}

.site-footer__main {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 32px;
}

.site-footer__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.site-footer__mark {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 6px;
  border: 1px solid var(--gcc-border-strong);
  color: var(--gcc-text);
  font-family: var(--gcc-mono);
  font-size: 13px;
}

.site-footer__brand-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.site-footer__game {
  color: var(--gcc-text);
  font-size: 13.5px;
  font-weight: 500;
  line-height: 1.3;
}

.site-footer__meta {
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

.site-footer__nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 24px;
}

.site-footer__link {
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  transition: color 0.15s ease;
}

.site-footer__link:hover {
  color: var(--gcc-text);
}

.site-footer__built {
  padding-top: 18px;
  border-top: 1px solid var(--gcc-border);
  font-family: var(--gcc-mono);
  font-size: 9px;
  letter-spacing: 0.16em;
  color: var(--gcc-muted);
  opacity: 0.7;
}

/* ---------------- responsive ---------------- */
@media (max-width: 900px) {
  .site-header__inner {
    gap: 14px;
  }

  .site-nav {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    display: none;
    flex-direction: column;
    align-items: stretch;
    gap: 0;
    padding: 10px 32px 16px;
    border-bottom: 1px solid var(--gcc-border);
    background: var(--gcc-header-bg);
    backdrop-filter: blur(10px);
    max-height: calc(100vh - 64px);
    overflow-y: auto;
  }

  .site-nav--open {
    display: flex;
  }

  .site-nav__link {
    padding: 12px 0;
    border-bottom: 1px solid var(--gcc-border);
  }

  .site-nav__link--current {
    border-bottom-color: var(--gcc-border);
    color: var(--gcc-accent);
  }

  .site-header__menu {
    display: inline-flex;
  }

  .site-header__gallery {
    display: none;
  }

  .theme-toggle__label {
    display: none;
  }

  .site-header__inner {
    position: relative;
  }
}

@media (max-width: 560px) {
  .site-header__inner {
    gap: 10px;
  }

  .site-footer__main {
    align-items: flex-start;
  }
}
</style>

<!--
  门户主题下通用类的适配（非 scoped，选择器限定在 .site-layout 内，不影响管理端）：
  固定视图（详情页）里的链接/按钮/输入框等仍使用 --gcc-* 主题变量。
-->
<style>
.site-layout .gcc-btn {
  border-color: var(--gcc-border-strong);
  background: transparent;
  color: var(--gcc-text);
  border-radius: 9999px;
}

.site-layout .gcc-btn:hover:not(:disabled) {
  border-color: var(--gcc-text);
  background: var(--gcc-hover);
  color: var(--gcc-text);
}

.site-layout .gcc-btn-primary {
  border-color: var(--gcc-text);
  background: var(--gcc-text);
  color: var(--gcc-bg);
}

.site-layout .gcc-btn-primary:hover:not(:disabled) {
  border-color: var(--gcc-text);
  background: var(--gcc-text);
  color: var(--gcc-bg);
  opacity: 0.9;
}

.site-layout .gcc-btn-ghost {
  border-color: transparent;
  color: var(--gcc-muted);
}

.site-layout .gcc-btn-ghost:hover:not(:disabled) {
  color: var(--gcc-text);
  background: var(--gcc-hover);
}

.site-layout .gcc-badge {
  border-color: var(--gcc-border-strong);
  background: var(--gcc-badge-bg);
  color: var(--gcc-muted);
}

.site-layout .gcc-input,
.site-layout .gcc-textarea,
.site-layout .gcc-select {
  background: var(--gcc-input-bg);
  border-color: var(--gcc-border);
  color: var(--gcc-text);
  border-radius: 8px;
}

.site-layout .gcc-input:focus,
.site-layout .gcc-textarea:focus,
.site-layout .gcc-select:focus {
  border-color: var(--gcc-border-strong);
}

.site-layout .gcc-label {
  color: var(--gcc-muted);
}

.site-layout .gcc-card {
  border-color: var(--gcc-border);
  border-radius: var(--gcc-card-radius, 8px);
  background: var(--gcc-card-bg);
  box-shadow: none;
}

.site-layout .gcc-empty,
.site-layout .gcc-loading,
.site-layout .gcc-error {
  border: 1px solid var(--gcc-border);
  border-radius: var(--gcc-card-radius, 8px);
  background: transparent;
  color: var(--gcc-muted);
}
</style>
