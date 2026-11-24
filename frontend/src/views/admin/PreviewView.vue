<script setup lang="ts">
/**
 * 草稿预览页（/admin/projects/:id/pages/:pageId/preview，workspace 路由）。
 *
 * 与公开页的区别（docs/API.md §8）：
 * - 配置读「草稿」（GET /api/projects/{id}/pages/{pageId}/page），不需要先发布；
 * - 数据走管理端真实内容接口（createDraftLoader；更新日志/文档只取 PUBLISHED）；
 * - 顶部 workspace 工具栏（xAI 控制台风格）：草稿预览标识 + 返回编辑器 / 页面管理 /
 *   访问公开站点 / 发布页面；页内不再重复 AdminUserMenu（AdminShell 顶栏已有）；
 * - 预览内容仍是**门户主题**（运行时内联 --gcc-* 变量）；工具栏使用管理端 xAI 令牌；
 * - 按钮动作按**预览路由**跳转（executeAction mode='preview'），不会泄漏到公开端；
 * - 页脚规则与公开端一致（BUGFIX_PLAN §1）：页面配置含可见 footer 组件时只渲染该页脚，
 *   否则渲染一份兜底站点页脚，保证全页 footer 恰好 1 个；
 * - 可直接发布当前页面，发布成功后更新状态并给出公开入口。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { isApiError } from '@/api/client'
import { getPage, getPageDraft, listPages, publishPage } from '@/api/pages'
import { getProject } from '@/api/projects'
import { getSiteSettings } from '@/api/site'
import XIcon from '@/components/admin/XIcon.vue'
import CommunityRuntime from '@/runtime/CommunityRuntime.vue'
import { toErrorMessage } from '@/runtime/errors'
import { createDraftLoader } from '@/runtime/loaders'
import {
  buildThemeStyle,
  ensureSystemThemeListener,
  systemPrefersDark,
} from '@/runtime/themeContext'
import { DEFAULT_THEME } from '@/types/page'
import type { ProjectView } from '@/types/api'
import type {
  PageStatus,
  SitePageDraftEnvelope,
  SitePageView,
  SiteRuntimeContext,
  SiteSettings,
} from '@/types/site'
import type { RuntimeDataLoader } from '@/types/runtime'
import { pathForPage } from '@/utils/actions'
import { formatDateTime } from '@/utils/format'
import { resolveThemeMode } from '@/utils/theme'

const route = useRoute()

function routeParam(value: unknown): string {
  const raw = Array.isArray(value) ? value[0] : value
  return typeof raw === 'string' ? raw : ''
}

const projectId = computed(() => {
  const numeric = Number(routeParam(route.params.id))
  return Number.isInteger(numeric) && numeric > 0 ? numeric : 0
})

const pageId = computed(() => routeParam(route.params.pageId))

const project = ref<ProjectView | null>(null)
const page = ref<SitePageView | null>(null)
const pages = ref<SitePageView[]>([])
const settings = ref<SiteSettings | null>(null)
const draft = ref<SitePageDraftEnvelope | null>(null)

const loading = ref(true)
const notFound = ref(false)
const errorMessage = ref('')

const publishing = ref(false)
const publishMessage = ref('')
const publishError = ref('')

const loader = computed<RuntimeDataLoader>(() => createDraftLoader(projectId.value))

const site = computed<SiteRuntimeContext>(() => ({
  projectId: project.value?.id ?? projectId.value,
  projectSlug: project.value?.slug ?? '',
  pages: pages.value.map((item) => ({
    pageId: item.pageId,
    name: item.name,
    slug: item.slug,
    pageType: item.pageType,
    status: item.status,
  })),
  themeMode: settings.value?.themeMode ?? 'DARK',
}))

const config = computed(() => draft.value?.config ?? null)

/** 页面配置里是否有可见 footer 组件（与 PublicPageShell 的页脚规则保持一致） */
const configHasVisibleFooter = computed(() => {
  const components = config.value?.components
  if (!Array.isArray(components)) return false
  return components.some((item) => item && item.type === 'footer' && item.visible !== false)
})

/** 预览画布背景跟随门户主题（运行时自身也会覆盖 --gcc-* 变量） */
const canvasStyle = computed<Record<string, string>>(() => {
  const effective = resolveThemeMode(
    settings.value?.themeMode ?? 'DARK',
    null,
    systemPrefersDark.value,
  )
  return buildThemeStyle(config.value?.theme ?? DEFAULT_THEME, effective)
})

/** 页面已发布且有项目 slug 时给出公开入口 */
const publicPath = computed(() => {
  const current = page.value
  const currentProject = project.value
  if (!current || !currentProject) return ''
  if (current.status !== 'PUBLISHED') return ''
  return pathForPage(current, currentProject.slug)
})

const versionSummary = computed(() => {
  const current = page.value
  if (!current) return ''
  const published =
    current.publishedVersion === null ? '尚未发布' : `已发布 v${current.publishedVersion}`
  const draftVersion = draft.value?.configVersion ?? current.draftVersion
  return `草稿 v${draftVersion} · ${published}`
})

const currentYear = new Date().getFullYear()

let loadSeq = 0

onMounted(() => ensureSystemThemeListener())

async function loadPreview(): Promise<void> {
  const seq = loadSeq + 1
  loadSeq = seq
  loading.value = true
  notFound.value = false
  errorMessage.value = ''
  publishMessage.value = ''
  publishError.value = ''

  const id = projectId.value
  const currentPageId = pageId.value
  if (!id || !currentPageId) {
    notFound.value = true
    errorMessage.value = '页面不存在或已被删除'
    project.value = null
    page.value = null
    draft.value = null
    loading.value = false
    document.title = '草稿预览 · 页面不存在'
    return
  }

  try {
    const [projectData, pageData, pageList, settingsData, draftData] = await Promise.all([
      getProject(id),
      getPage(id, currentPageId),
      listPages(id),
      getSiteSettings(id),
      getPageDraft(id, currentPageId),
    ])
    if (seq !== loadSeq) return
    project.value = projectData
    page.value = pageData
    pages.value = Array.isArray(pageList) ? pageList : []
    settings.value = settingsData
    draft.value = draftData
    document.title = `草稿预览 · ${pageData.name}`
  } catch (error) {
    if (seq !== loadSeq) return
    project.value = null
    page.value = null
    draft.value = null
    if (isApiError(error) && error.status === 404) {
      notFound.value = true
      errorMessage.value = '页面不存在或已被删除'
      document.title = '草稿预览 · 页面不存在'
    } else {
      errorMessage.value = toErrorMessage(error, '草稿预览加载失败，请稍后重试')
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

async function handlePublish(): Promise<void> {
  const currentPage = page.value
  const currentProject = project.value
  if (publishing.value || !currentPage || !currentProject) return
  publishing.value = true
  publishMessage.value = ''
  publishError.value = ''
  try {
    const result = await publishPage(currentProject.id, currentPage.pageId)
    publishMessage.value = `发布成功：${currentPage.name} v${result.version}（${formatDateTime(
      result.publishedAt,
    )}）`
    const nextStatus: PageStatus = 'PUBLISHED'
    page.value = {
      ...currentPage,
      status: nextStatus,
      publishedVersion: result.version,
    }
    project.value = { ...currentProject, status: 'PUBLISHED' }
  } catch (error) {
    publishError.value = toErrorMessage(error, '发布失败，请稍后重试')
  } finally {
    publishing.value = false
  }
}

watch(projectId, () => void loadPreview(), { immediate: true })
watch(pageId, () => void loadPreview())
</script>

<template>
  <div class="preview-workspace">
    <header class="preview-toolbar">
      <div class="preview-toolbar__identity">
        <span class="preview-toolbar__badge">DRAFT PREVIEW</span>
        <div class="preview-toolbar__title">
          <p class="preview-toolbar__page">{{ page?.name ?? '页面预览' }}</p>
          <p class="preview-toolbar__meta">
            {{ project?.name ?? '未命名站点' }}
            <template v-if="versionSummary"> · {{ versionSummary }}</template>
            <span class="preview-toolbar__hint">· 草稿预览 · 以下内容尚未发布，仅管理员可见</span>
          </p>
        </div>
      </div>

      <div class="preview-toolbar__actions">
        <RouterLink
          v-if="project && page"
          class="gcc-btn gcc-btn-sm"
          :to="`/admin/projects/${project.id}/pages/${page.pageId}/editor`"
        >
          <XIcon name="edit" :size="13" />
          返回编辑器
        </RouterLink>
        <RouterLink
          v-if="project"
          class="gcc-btn gcc-btn-sm gcc-btn-ghost"
          :to="`/admin/projects/${project.id}/pages`"
        >
          页面管理
        </RouterLink>
        <a
          v-if="project && publicPath"
          class="gcc-btn gcc-btn-sm gcc-btn-ghost"
          :href="publicPath"
          target="_blank"
          rel="noopener noreferrer"
        >
          访问公开站点
          <XIcon name="arrow-up-right" :size="12" />
        </a>
        <button
          class="gcc-btn gcc-btn-sm gcc-btn-primary"
          type="button"
          :disabled="publishing || !project || !page"
          @click="handlePublish"
        >
          <XIcon name="publish" :size="13" />
          {{ publishing ? '发布中…' : '发布页面' }}
        </button>
      </div>
    </header>

    <div v-if="publishMessage || publishError" class="preview-feedback">
      <p v-if="publishMessage" class="preview-feedback__ok" role="status">
        {{ publishMessage }}
        <a v-if="publicPath" class="preview-feedback__link" :href="publicPath" target="_blank" rel="noopener noreferrer">
          打开公开页面 →
        </a>
      </p>
      <p v-else class="preview-feedback__error" role="alert">{{ publishError }}</p>
    </div>

    <div class="preview-canvas" :style="canvasStyle">
      <div v-if="loading" class="preview-state">
        <div class="preview-state__panel">
          <p class="preview-state__kicker">DRAFT PREVIEW</p>
          <p class="preview-state__text">正在加载草稿配置与预览数据…</p>
        </div>
      </div>

      <div v-else-if="notFound" class="preview-state">
        <div class="preview-state__panel">
          <p class="preview-state__kicker">404 · PAGE NOT FOUND</p>
          <h1 class="preview-state__title">页面不存在或已被删除</h1>
          <p class="preview-state__text">请检查访问地址是否正确，或返回页面管理重新选择页面。</p>
          <RouterLink
            class="gcc-btn gcc-btn-sm gcc-btn-primary"
            :to="projectId ? `/admin/projects/${projectId}/pages` : '/admin/projects'"
          >
            返回页面管理
          </RouterLink>
        </div>
      </div>

      <div v-else-if="errorMessage" class="preview-state">
        <div class="preview-state__panel">
          <p class="preview-state__kicker">ERROR · LOAD FAILED</p>
          <h1 class="preview-state__title">草稿预览加载失败</h1>
          <p class="preview-state__text">{{ errorMessage }}</p>
          <button class="gcc-btn gcc-btn-sm" type="button" @click="loadPreview">重试</button>
        </div>
      </div>

      <template v-else-if="config">
        <CommunityRuntime
          :key="`preview-${pageId}-${draft?.configVersion ?? 0}`"
          :config="config"
          :loader="loader"
          :site="site"
          mode="preview"
        />
        <!-- 页脚兜底：页面配置没有 footer 组件时补一份站点页脚，保证预览与公开端一致（恰好 1 个） -->
        <footer v-if="!configHasVisibleFooter" class="preview-fallback-footer" data-layout-footer>
          <div class="gcc-container">
            © {{ currentYear }} {{ project?.name ?? '游戏站点' }} · 由 GameCommunityCreator 搭建
          </div>
        </footer>
      </template>
    </div>
  </div>
</template>

<style scoped>
.preview-workspace {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--x-canvas);
  color: var(--x-text);
}

/* ---------------- workspace 工具栏 ---------------- */
.preview-toolbar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 10px 24px;
  border-bottom: 1px solid var(--x-border);
  background: var(--gcc-header-bg);
  backdrop-filter: blur(10px);
}

.preview-toolbar__identity {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.preview-toolbar__badge {
  flex: none;
  padding: 3px 10px;
  border-radius: 9999px;
  border: 1px solid rgba(255, 122, 23, 0.45);
  background: var(--x-accent-soft);
  color: #ffe3cf;
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.14em;
}

.preview-toolbar__title {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.preview-toolbar__page {
  color: var(--x-text);
  font-size: 13.5px;
  font-weight: 500;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-toolbar__meta {
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--x-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-toolbar__hint {
  opacity: 0.85;
}

.preview-toolbar__actions {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-feedback {
  flex: none;
  padding: 8px 24px;
  border-bottom: 1px solid var(--x-border);
  background: var(--x-surface);
  font-size: 12px;
}

.preview-feedback__ok {
  color: var(--x-success);
}

.preview-feedback__link {
  margin-left: 8px;
  color: var(--x-text);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.preview-feedback__error {
  color: #ff8d8b;
}

/* ---------------- 预览画布（门户主题） ---------------- */
.preview-canvas {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.preview-canvas :deep(.gcc-runtime) {
  flex: 1;
}

.preview-fallback-footer {
  border-top: 1px solid var(--gcc-border);
  background: var(--gcc-card-bg);
  color: var(--gcc-muted);
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.08em;
  text-align: center;
  padding: 18px 0 24px;
}

/* ---------------- 状态 ---------------- */
.preview-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
}

.preview-state__panel {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  max-width: 560px;
  padding: 32px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
}

.preview-state__kicker {
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.16em;
  color: var(--x-muted);
}

.preview-state__title {
  color: var(--x-text);
  font-size: 22px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.preview-state__text {
  color: var(--x-muted);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .preview-toolbar__hint {
    display: none;
  }
}

/* 管理端浅色主题下的对比度适配（预览内容仍由门户主题控制） */
:global(.admin-theme-scope[data-admin-theme='light']) .preview-toolbar__badge {
  color: #9a3412;
  border-color: rgba(154, 52, 18, 0.28);
  background: rgba(255, 122, 23, 0.1);
}

:global(.admin-theme-scope[data-admin-theme='light']) .preview-feedback__ok {
  color: #0f7a4e;
}

:global(.admin-theme-scope[data-admin-theme='light']) .preview-feedback__error {
  color: #b42318;
}

@media (max-width: 900px) {
  .preview-toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .preview-toolbar__actions {
    flex-wrap: wrap;
  }
}
</style>
