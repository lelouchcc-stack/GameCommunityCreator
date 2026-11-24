<script setup lang="ts">
/**
 * 页面级可视化编辑器（路由 /admin/projects/:id/pages/:pageId/editor）。
 *
 * 布局：顶部工具栏 + 三栏主体（左 264px 组件库/页面结构 · 中自适应画布 · 右 340px 属性/页面设置）。
 * 画布复用 CommunityRuntime.vue（mode="editor"）+ createDraftLoader(projectId)，site 上下文
 * = { projectId, projectSlug, pages, themeMode }，按钮动作在 editor 模式下不真正跳转。
 *
 * 本文件只做编排：加载、保存（页面级草稿乐观锁）、发布（页面级）、重置模板、快捷键、未保存离开确认。
 */
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import ComponentPalette from '@/editor/ComponentPalette.vue'
import EditorCanvas from '@/editor/EditorCanvas.vue'
import PageStructure from '@/editor/PageStructure.vue'
import PropertyPanel from '@/editor/PropertyPanel.vue'
import ToastHost from '@/editor/ToastHost.vue'
import { useEditorState } from '@/editor/useEditorState'
import { useToast } from '@/editor/useToast'
import { createDraftLoader } from '@/runtime/loaders'
import { formatDateTime } from '@/utils/format'
import { PAGE_ID_PATTERN, pathForPage } from '@/utils/actions'
import { PAGE_STATUS_LABELS } from '@/types/site'
import type { ComponentPropValue, ComponentType, PageAction, ThemeConfig } from '@/types/page'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const rawId = Number(route.params.id)
const invalidId = !Number.isInteger(rawId) || rawId <= 0
const projectId = invalidId ? 0 : rawId

const rawPageId = typeof route.params.pageId === 'string' ? route.params.pageId : ''
const invalidPageId = !PAGE_ID_PATTERN.test(rawPageId)
const pageId = invalidPageId ? '' : rawPageId

const {
  project,
  page,
  pages,
  config,
  configVersion,
  lastSavedAt,
  selectedId,
  selectedComponent,
  dirty,
  hasUnpublishedVersion,
  pageTypeLabel,
  siteContext,
  loadWarnings,
  loading,
  loadError,
  loadErrorStatus,
  saving,
  publishing,
  resetting,
  saveError,
  conflict,
  fieldErrors,
  validationErrors,
  validationWarnings,
  load,
  reloadFromServer,
  select,
  addComponent,
  removeComponent,
  moveComponent,
  moveUp,
  moveDown,
  toggleVisible,
  updateComponentTitle,
  updateProp,
  updateAction,
  updatePageTitle,
  updateTheme,
  applyThemePreset,
  save,
  publish,
  resetDefault,
  clearMessages,
} = useEditorState(projectId, pageId)

const loader = createDraftLoader(projectId)

/** 当前页面在公开端的路径（只读展示，仍未发布时仅作参考） */
const publicPath = computed(() => {
  if (!page.value || !project.value) return ''
  return pathForPage(page.value, project.value.slug)
})

const pageStatusBadgeClass = computed(() => {
  const status = page.value?.status
  if (status === 'PUBLISHED') return 'gcc-badge-success'
  if (status === 'DISABLED') return 'gcc-badge-danger'
  return 'gcc-badge-warning'
})

const saveFieldErrorList = computed(() =>
  Object.entries(fieldErrors.value).map(([key, message]) => `${key}：${message}`),
)

const hasAlert = computed(
  () =>
    conflict.value ||
    Boolean(saveError.value) ||
    validationErrors.value.length > 0 ||
    saveFieldErrorList.value.length > 0,
)

const alertTitle = computed(() => {
  if (conflict.value) return '保存冲突（409）'
  if (validationErrors.value.length > 0) return '页面配置校验未通过'
  return '保存失败'
})

const activeWarnings = computed(() => [...loadWarnings.value, ...validationWarnings.value])

/* ---------------- 组件/页面编辑 ---------------- */
function onAddComponent(type: ComponentType): void {
  addComponent(type)
}

function onRemoveComponent(id: string): void {
  removeComponent(id)
}

function onUpdateComponentTitle(value: string): void {
  if (selectedId.value) updateComponentTitle(selectedId.value, value)
}

function onUpdateProp(key: string, value: ComponentPropValue): void {
  if (selectedId.value) updateProp(selectedId.value, key, value)
}

function onUpdateAction(action: PageAction): void {
  if (selectedId.value) updateAction(selectedId.value, action)
}

function onToggleSelectedVisible(value: boolean): void {
  if (!selectedId.value) return
  const current = selectedComponent.value
  if (current && current.visible !== value) toggleVisible(selectedId.value)
}

function onUpdateTheme(patch: Partial<ThemeConfig>): void {
  updateTheme(patch)
}

function onApplyPreset(theme: ThemeConfig): void {
  applyThemePreset(theme)
}

/* ---------------- 保存 / 发布 / 重置 / 预览 ---------------- */
async function handleSave(): Promise<void> {
  if (saving.value) return
  if (!dirty.value) {
    toast.info('没有需要保存的修改')
    return
  }
  await save()
}

function goBack(): void {
  void router.push({ name: 'page-list', params: { id: projectId } })
}

function goSettings(): void {
  void router.push({ name: 'site-settings', params: { id: projectId } })
}

function openPreview(): void {
  if (
    dirty.value &&
    !window.confirm('有未保存的修改：预览显示的是上次保存的草稿版本。建议先保存。仍要打开预览？')
  ) {
    return
  }
  const href = router.resolve({
    name: 'page-preview',
    params: { id: projectId, pageId },
  }).href
  window.open(href, '_blank', 'noopener')
}

async function onPublish(): Promise<void> {
  if (!hasUnpublishedVersion.value) {
    toast.info('当前草稿版本已发布，修改并保存产生新版本后可再次发布')
    return
  }
  const message = dirty.value
    ? '当前有未保存的修改：发布使用的是上次保存的草稿版本（未保存的修改不会发布）。建议先保存。仍要发布？'
    : `确认发布页面「${page.value?.name ?? ''}」草稿 v${configVersion.value}？发布后公开页立即更新，不影响其他页面。`
  if (!window.confirm(message)) return
  await publish()
}

async function onReset(): Promise<void> {
  const template = page.value?.templateKey ?? '默认模板'
  const message = dirty.value
    ? `当前有未保存的修改。重置会丢弃本地修改并按模板「${template}」恢复。确定继续？`
    : `重置会按模板「${template}」恢复草稿（不影响已发布快照）。确定继续？`
  if (!window.confirm(message)) return
  await resetDefault()
}

async function onReloadServer(): Promise<void> {
  if (
    dirty.value &&
    !window.confirm('刷新将丢弃本地未保存的修改，并载入服务器上的最新配置。确定继续？')
  ) {
    return
  }
  await reloadFromServer()
}

async function onRetryLoad(): Promise<void> {
  await load()
}

/* ---------------- 快捷键 / 离开确认 ---------------- */
function onKeydown(event: KeyboardEvent): void {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    event.preventDefault()
    void handleSave()
  }
}

function onBeforeUnload(event: BeforeUnloadEvent): void {
  if (!dirty.value) return
  event.preventDefault()
  event.returnValue = ''
}

onMounted(() => {
  if (!invalidId && !invalidPageId) void load()
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('beforeunload', onBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('beforeunload', onBeforeUnload)
})

onBeforeRouteLeave((to) => {
  // 登录失效等跳登录页的强制跳转不弹确认（服务端 Session 已失效，保存也只会 401）
  if (to.name === 'auth-login') return true
  if (!dirty.value) return true
  return window.confirm('当前页面有未保存的修改，确定离开吗？未保存的修改将丢失。')
})
</script>

<template>
  <div class="editor-page">
    <ToastHost />

    <!-- 路由参数非法 -->
    <div v-if="invalidId || invalidPageId" class="editor-fallback">
      <div class="fallback-card gcc-card">
        <h1>无法打开编辑器</h1>
        <p class="gcc-muted">
          地址中的{{ invalidId ? '站点 ID' : '页面 ID' }}不合法（{{
            invalidId ? route.params.id : route.params.pageId
          }}），请从页面管理进入。
        </p>
        <div class="fallback-actions">
          <button type="button" class="gcc-btn gcc-btn-primary" @click="goBack">返回页面管理</button>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-else-if="loading && !config" class="editor-fallback">
      <div class="loading-block">
        <span class="spinner" aria-hidden="true" />
        <p class="gcc-muted">正在加载站点、页面元信息与草稿配置…</p>
      </div>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="loadError" class="editor-fallback">
      <div class="fallback-card gcc-card">
        <h1>{{ loadErrorStatus === 404 ? '页面不存在' : '编辑器加载失败' }}</h1>
        <p class="error-text">{{ loadError }}</p>
        <div class="fallback-actions">
          <button type="button" class="gcc-btn gcc-btn-primary" @click="onRetryLoad">重试</button>
          <button type="button" class="gcc-btn" @click="goBack">返回页面管理</button>
        </div>
      </div>
    </div>

    <!-- 编辑器主体 -->
    <div v-else-if="config && project && page && siteContext" class="editor-shell">
      <header class="editor-topbar">
        <div class="topbar-left">
          <button type="button" class="gcc-btn gcc-btn-sm gcc-btn-ghost back-btn" @click="goBack">
            ← 页面管理
          </button>
          <span class="topbar-divider" aria-hidden="true" />
          <span class="x-kicker topbar-kicker">PAGE EDITOR</span>
          <div class="topbar-project">
            <div class="topbar-title">
              <strong class="project-name" :title="project.name">{{ project.name }}</strong>
              <span class="topbar-sep" aria-hidden="true">/</span>
              <strong class="page-name" :title="page.name">{{ page.name }}</strong>
              <span class="gcc-badge">{{ pageTypeLabel }}</span>
              <span class="gcc-badge" :class="pageStatusBadgeClass">
                {{ PAGE_STATUS_LABELS[page.status] }}
              </span>
              <span v-if="page.isHome" class="gcc-badge gcc-badge-primary">首页</span>
              <span v-if="dirty" class="dirty-flag" title="有未保存的修改">● 未保存</span>
              <span v-else class="saved-flag" title="本地修改已保存">✓ 已保存</span>
            </div>
            <div class="topbar-sub x-mono">
              <span>{{ project.gameName }}</span>
              <span class="dot">·</span>
              <span class="gcc-mono">{{ project.slug }}</span>
              <span class="dot">·</span>
              <span class="gcc-mono">{{ publicPath }}</span>
              <span class="dot">·</span>
              <span>草稿 v{{ configVersion }}</span>
              <span class="dot">·</span>
              <span>{{
                page.publishedVersion === null ? '尚未发布' : `发布 v${page.publishedVersion}`
              }}</span>
              <template v-if="lastSavedAt">
                <span class="dot">·</span>
                <span>最近保存 {{ formatDateTime(lastSavedAt) }}</span>
              </template>
            </div>
          </div>
        </div>

        <div class="topbar-actions">
          <button
            type="button"
            class="gcc-btn gcc-btn-sm gcc-btn-ghost"
            title="打开站点设置（导航与主题）"
            @click="goSettings"
          >
            站点设置
          </button>
          <button
            type="button"
            class="gcc-btn gcc-btn-sm"
            :disabled="resetting"
            title="按页面模板重置草稿（不影响已发布快照）"
            @click="onReset"
          >
            {{ resetting ? '重置中…' : '重置模板' }}
          </button>
          <button
            type="button"
            class="gcc-btn gcc-btn-sm"
            title="在新窗口打开该页面的草稿预览"
            @click="openPreview"
          >
            草稿预览
          </button>
          <button
            type="button"
            class="gcc-btn gcc-btn-sm gcc-btn-primary"
            :disabled="saving || !dirty"
            :title="!dirty ? '没有需要保存的修改' : '保存草稿（Ctrl/Cmd + S）'"
            @click="handleSave"
          >
            {{ saving ? '保存中…' : '保存' }}
            <kbd class="kbd">Ctrl/⌘ S</kbd>
          </button>
          <button
            type="button"
            class="gcc-btn gcc-btn-sm publish-btn"
            :disabled="publishing || !hasUnpublishedVersion"
            :title="
              hasUnpublishedVersion
                ? '发布当前页面的草稿版本（不影响其他页面）'
                : '当前草稿版本已发布，修改并保存后可再次发布'
            "
            @click="onPublish"
          >
            {{ publishing ? '发布中…' : '发布页面' }}
          </button>
        </div>
      </header>

      <!-- 校验告警 / 保存错误 -->
      <div v-if="hasAlert" class="editor-error-bar">
        <div class="alert-body">
          <strong class="alert-title">{{ alertTitle }}</strong>
          <span v-if="conflict" class="alert-text">
            配置已被其他操作修改，请刷新后重试（刷新会放弃本地未保存的修改）。
          </span>
          <span v-else-if="validationErrors.length === 0" class="alert-text">{{ saveError }}</span>
          <span v-else class="alert-text">请按下列提示修正后再保存：</span>
          <ul v-if="validationErrors.length > 0" class="alert-list">
            <li v-for="item in validationErrors" :key="item">{{ item }}</li>
          </ul>
          <ul v-else-if="saveFieldErrorList.length > 0" class="alert-list">
            <li v-for="item in saveFieldErrorList" :key="item">{{ item }}</li>
          </ul>
        </div>
        <div class="alert-actions">
          <button v-if="conflict" type="button" class="gcc-btn gcc-btn-sm" @click="onReloadServer">
            刷新配置
          </button>
          <button type="button" class="gcc-btn gcc-btn-sm gcc-btn-ghost" @click="clearMessages">
            关闭
          </button>
        </div>
      </div>

      <!-- 可保存的警告（例如按钮目标页尚未发布） -->
      <div v-if="activeWarnings.length > 0" class="editor-warning">
        <div class="warning-body">
          <strong class="warning-title">提示（不阻止保存）</strong>
          <ul class="warning-list">
            <li v-for="item in activeWarnings" :key="item">{{ item }}</li>
          </ul>
        </div>
        <button type="button" class="gcc-btn gcc-btn-sm gcc-btn-ghost" @click="clearMessages">
          已知晓
        </button>
      </div>

      <div class="editor-body">
        <aside class="editor-left">
          <ComponentPalette :count="config.components.length" @add="onAddComponent" />
          <PageStructure
            :components="config.components"
            :selected-id="selectedId"
            @select="select"
            @move-up="moveUp"
            @move-down="moveDown"
            @remove="onRemoveComponent"
            @toggle-visible="toggleVisible"
            @reorder="moveComponent"
          />
        </aside>

        <main class="editor-center">
          <EditorCanvas
            :config="config"
            :loader="loader"
            :selected-id="selectedId"
            :site="siteContext"
            @select="select"
          />
        </main>

        <aside class="editor-right">
          <PropertyPanel
            :config="config"
            :selected="selectedComponent"
            :pages="pages"
            :page-id="pageId"
            @update-page-title="updatePageTitle"
            @update-theme="onUpdateTheme"
            @apply-preset="onApplyPreset"
            @update-component-title="onUpdateComponentTitle"
            @update-prop="onUpdateProp"
            @toggle-visible="onToggleSelectedVisible"
            @update-action="onUpdateAction"
          />
        </aside>
      </div>
    </div>

    <div v-else class="editor-fallback">
      <div class="fallback-card gcc-card">
        <h1>编辑器暂无内容</h1>
        <p class="gcc-muted">未能读取到站点、页面或草稿配置，请返回页面管理后重试。</p>
        <div class="fallback-actions">
          <button type="button" class="gcc-btn gcc-btn-primary" @click="goBack">返回页面管理</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Editor 是 shell 的 workspace 路由：根容器占满剩余高度，自身滚动，不能出现双滚动条 */
.editor-page {
  --page-danger-fg: var(--x-danger);
  --page-warning-fg: var(--x-warning);
  --page-success-fg: var(--x-success);
  /* shell workspace：顶栏 64px，本页占满剩余视口并自行内部滚动（避免页面级双滚动条） */
  flex: none;
  min-height: 0;
  height: calc(100vh - 64px);
  max-height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--x-canvas);
}

/* ---------- 全局状态 ---------- */
.editor-fallback {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
}

.fallback-card {
  width: min(520px, 100%);
  padding: 28px 30px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border: 1px solid var(--x-border-strong);
  border-radius: var(--x-radius);
  background: var(--x-surface);
}

.fallback-card h1 {
  font-size: 22px;
  font-weight: 400;
  letter-spacing: -0.02em;
}

.error-text {
  font-size: 13px;
  color: var(--page-danger-fg);
}

.fallback-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

.loading-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.spinner {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2px solid var(--x-border-strong);
  border-top-color: var(--x-accent);
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ---------- 编辑器骨架 ---------- */
.editor-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 工程工具风 topbar：60px、hairline、mono 小标签 */
.editor-topbar {
  flex: none;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 16px;
  border-bottom: 1px solid var(--x-border);
  background: var(--x-canvas-soft);
  z-index: 5;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.back-btn {
  flex: none;
  padding: 5px 12px;
  font-size: 12px;
}

.topbar-divider {
  width: 1px;
  height: 22px;
  background: var(--x-border);
}

.topbar-kicker {
  flex: none;
  font-size: 9px;
  letter-spacing: 0.18em;
  opacity: 0.72;
}

.topbar-project {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.topbar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.project-name,
.page-name {
  font-size: 13.5px;
  font-weight: 500;
  letter-spacing: -0.005em;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-name {
  color: var(--x-text);
}

.page-name {
  color: var(--x-text-secondary);
}

.topbar-sep {
  color: var(--x-muted);
}

.dirty-flag {
  flex: none;
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--page-warning-fg);
}

.saved-flag {
  flex: none;
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--page-success-fg);
}

.topbar-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 9.5px;
  letter-spacing: 0.06em;
  color: var(--x-muted);
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.topbar-sub .dot {
  opacity: 0.5;
}

.topbar-actions {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
}

.topbar-actions .gcc-btn {
  padding: 5px 12px;
  font-size: 11.5px;
}

.kbd {
  padding: 1px 5px;
  border-radius: 4px;
  border: 1px solid var(--x-border);
  background: transparent;
  font-size: 8.5px;
  letter-spacing: 0.04em;
  font-family: var(--x-mono);
  opacity: 0.7;
}

.publish-btn {
  border-color: rgba(255, 122, 23, 0.45);
  color: var(--x-accent);
}

.publish-btn:hover:not(:disabled) {
  border-color: rgba(255, 122, 23, 0.7);
  background: var(--x-accent-soft);
}

/* ---------- 提示条 ---------- */
.editor-error-bar {
  flex: none;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 16px;
  border-bottom: 1px solid rgba(242, 95, 92, 0.35);
  background: rgba(242, 95, 92, 0.05);
}

.alert-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12.5px;
}

.alert-title {
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--page-danger-fg);
}

.alert-text {
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--x-text-secondary);
}

.alert-list {
  margin: 0;
  padding-left: 18px;
  color: var(--x-text-secondary);
  font-size: 12px;
  line-height: 1.7;
  max-height: 120px;
  overflow-y: auto;
}

.alert-actions {
  flex: none;
  display: flex;
  gap: 8px;
}

.alert-actions .gcc-btn {
  padding: 4px 11px;
  font-size: 11.5px;
}

.editor-warning {
  flex: none;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 9px 16px;
  border-bottom: 1px solid rgba(255, 176, 32, 0.3);
  background: rgba(255, 176, 32, 0.05);
}

.warning-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.warning-title {
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--page-warning-fg);
}

.warning-list {
  margin: 0;
  padding-left: 18px;
  font-size: 12px;
  line-height: 1.65;
  color: var(--x-text-secondary);
  max-height: 96px;
  overflow-y: auto;
}

.editor-warning .gcc-btn {
  padding: 4px 11px;
  font-size: 11.5px;
}

/* ---------- 三栏 ---------- */
.editor-body {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: stretch;
  overflow: hidden;
}

.editor-left {
  flex: none;
  width: 268px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid var(--x-border);
  background: var(--x-canvas-soft);
}

.editor-center {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.editor-right {
  flex: none;
  width: 344px;
  min-height: 0;
  overflow-y: auto;
  border-left: 1px solid var(--x-border);
  background: var(--x-canvas-soft);
}

:global(.admin-theme-scope[data-admin-theme='light'] .editor-page) {
  --page-danger-fg: #b42318;
  --page-warning-fg: #8a5a00;
  --page-success-fg: #0f7a4e;
}

/* 1280px 宽屏下仍保证三栏可用：收窄左右栏 */
@media (max-width: 1440px) {
  .editor-left {
    width: 240px;
  }

  .editor-right {
    width: 316px;
  }
}

@media (max-width: 1280px) {
  .project-name,
  .page-name {
    max-width: 150px;
  }

  .topbar-sub .x-mono {
    display: none;
  }
}
</style>
