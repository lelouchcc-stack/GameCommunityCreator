<script setup lang="ts">
/**
 * 按钮动作配置面板（PAGE_SCHEMA §4，v2 重点）。
 *
 * 只在选中 `button` 组件时由 PropertyPanel 渲染。规则：
 * - 动作类型三选一：无动作 / 站内跳转 / 外部链接（ACTION_TYPE_LABELS）；
 * - 切换类型时重建 action 对象，只保留当前类型允许的字段（不残留 targetPageId / url）；
 * - navigate：站点页面下拉（首页/模块/自定义 + 状态），DRAFT 标注「未发布」仍可选，DISABLED 禁选；
 * - externalLink：URL 输入 + 新标签开关（默认开），isValidHttpUrl 即时提示；
 * - none：提示运行时不可点击；
 * - 配置中已失效（页面被删除/停用）的 targetPageId 显示明显错误。
 */
import { computed } from 'vue'
import { getComponentDef } from '@/components/registry'
import {
  ACTION_TYPE_LABELS,
  ACTION_TYPES,
  type ActionType,
  type PageAction,
  type PageComponent,
} from '@/types/page'
import type { SitePageView } from '@/types/site'
import { PAGE_STATUS_LABELS, PAGE_TYPE_LABELS } from '@/types/site'
import { isValidHttpUrl, validateAction, type ActionTargetPage } from '@/utils/actions'

const props = defineProps<{
  component: PageComponent
  /** 站点全部页面（动作目标下拉） */
  pages: SitePageView[]
}>()

const emit = defineEmits<{ (event: 'update-action', action: PageAction): void }>()

const supportsAction = computed(() => getComponentDef(props.component.type).supportsAction)

const action = computed<PageAction>(() => props.component.action ?? { type: 'none' })

/** 下拉框当前值：非法/缺失动作回退到 none，并单独提示 */
const actionType = computed<ActionType>(() =>
  ACTION_TYPES.includes(action.value.type) ? action.value.type : 'none',
)

const targetPages = computed<ActionTargetPage[]>(() =>
  props.pages.map((item) => ({
    pageId: item.pageId,
    name: item.name,
    pageType: item.pageType,
    status: item.status,
    slug: item.slug,
  })),
)

/** 前端即时校验：errors 阻止保存，warnings 允许保存但提示 */
const validation = computed(() => validateAction(props.component.action, { targetPages: targetPages.value }))

const navigateTargetId = computed(() =>
  action.value.type === 'navigate' ? action.value.targetPageId : '',
)

const navigateTargetPage = computed(() =>
  props.pages.find((item) => item.pageId === navigateTargetId.value),
)

const externalUrl = computed(() => (action.value.type === 'externalLink' ? action.value.url : ''))

const externalUrlValid = computed(() => isValidHttpUrl(externalUrl.value))

const externalOpenInNewTab = computed(() =>
  action.value.type === 'externalLink' ? action.value.openInNewTab !== false : true,
)

const missingAction = computed(() => !props.component.action)

const targetError = computed(() => validation.value.errors.targetPageId ?? '')
const urlError = computed(() => validation.value.errors.url ?? '')
const targetWarning = computed(() => validation.value.warnings.targetPageId ?? '')
const typeError = computed(() => validation.value.errors.type ?? '')

function emitAction(next: PageAction): void {
  emit('update-action', next)
}

/** 切换类型：重建合法 action 对象，清理其他类型的字段 */
function onTypeChange(event: Event): void {
  const next = (event.target as HTMLSelectElement).value as ActionType
  if (next === 'navigate') {
    emitAction({ type: 'navigate', targetPageId: '' })
    return
  }
  if (next === 'externalLink') {
    emitAction({ type: 'externalLink', url: '', openInNewTab: true })
    return
  }
  emitAction({ type: 'none' })
}

function onTargetChange(event: Event): void {
  const value = (event.target as HTMLSelectElement).value
  emitAction({ type: 'navigate', targetPageId: value })
}

function onUrlInput(event: Event): void {
  emitAction({
    type: 'externalLink',
    url: (event.target as HTMLInputElement).value,
    openInNewTab: externalOpenInNewTab.value,
  })
}

function onOpenInNewTabChange(event: Event): void {
  emitAction({
    type: 'externalLink',
    url: externalUrl.value,
    openInNewTab: (event.target as HTMLInputElement).checked,
  })
}

function statusSuffix(page: SitePageView): string {
  const type = PAGE_TYPE_LABELS[page.pageType] ?? page.pageType
  const status = PAGE_STATUS_LABELS[page.status] ?? page.status
  const suffix = page.status === 'DRAFT' ? ' · 未发布' : ''
  return `${type} · ${status}${suffix}`
}
</script>

<template>
  <section v-if="supportsAction" class="action-panel">
    <header class="action-head">
      <h3 class="section-title">点击动作</h3>
      <span class="gcc-badge gcc-badge-primary">按钮专用</span>
    </header>
    <div class="gcc-field">
      <label class="gcc-label" for="action-type-select">动作类型</label>
      <select id="action-type-select" class="gcc-select" :value="actionType" @change="onTypeChange">
        <option v-for="type in ACTION_TYPES" :key="type" :value="type">
          {{ ACTION_TYPE_LABELS[type] }}
        </option>
      </select>
      <p v-if="typeError" class="hint-error">{{ typeError }}</p>
      <p v-if="missingAction" class="hint-error">
        该按钮尚未配置 action 字段，请选择动作类型（后端校验要求 button 必须带 action）。
      </p>
    </div>

    <!-- 站内跳转 -->
    <div v-if="actionType === 'navigate'" class="gcc-field action-block">
      <label class="gcc-label" for="action-target-select">
        目标页面 <span class="required">*</span>
      </label>
      <select
        id="action-target-select"
        class="gcc-select"
        :value="navigateTargetId"
        @change="onTargetChange"
      >
        <option value="" disabled>请选择要跳转的站内页面</option>
        <option
          v-for="item in pages"
          :key="item.pageId"
          :value="item.pageId"
          :disabled="item.status === 'DISABLED'"
          :title="item.status === 'DISABLED' ? '该页面已停用，不能作为跳转目标' : undefined"
        >
          {{ item.name }}（{{ statusSuffix(item) }}）
        </option>
        <!-- 已失效目标：页面被删除，或不在当前站点页面列表中 -->
        <option v-if="navigateTargetId && !navigateTargetPage" :value="navigateTargetId">
          ⚠ 已失效目标（{{ navigateTargetId }}）
        </option>
      </select>

      <p v-if="targetError" class="hint-error">⚠ {{ targetError }}</p>
      <p v-else-if="targetWarning" class="hint-warning">⚠ {{ targetWarning }}</p>
      <p v-else-if="navigateTargetPage" class="hint-ok">
        已选择「{{ navigateTargetPage.name }}」，点击后在公开页跳转到该页面（基于稳定 pageId，改 slug 不影响）。
      </p>
      <p v-else class="hint-muted">选择目标后，对话框下方「动作预览」会显示实际行为。</p>

      <p v-if="navigateTargetPage?.status === 'DISABLED'" class="hint-error">
        目标页面「{{ navigateTargetPage.name }}」已停用，保存会被后端拒绝，请重新选择。
      </p>
    </div>

    <!-- 外部链接 -->
    <div v-else-if="actionType === 'externalLink'" class="gcc-field action-block">
      <label class="gcc-label" for="action-url-input">
        外部链接 URL <span class="required">*</span>
      </label>
      <input
        id="action-url-input"
        class="gcc-input gcc-mono"
        type="text"
        maxlength="300"
        :value="externalUrl"
        placeholder="https://example.com"
        @input="onUrlInput"
      />
      <div class="field-meta">
        <span v-if="!externalUrl" class="hint-muted">只接受 http/https 绝对地址</span>
        <span v-else-if="externalUrlValid" class="hint-ok">✓ 链接格式合法</span>
        <span v-else class="hint-error">✕ 请输入合法的 http/https 绝对地址（禁止 javascript:/data:/相对地址）</span>
        <span>{{ externalUrl.length }}/300</span>
      </div>

      <label class="gcc-checkbox action-checkbox">
        <input type="checkbox" :checked="externalOpenInNewTab" @change="onOpenInNewTabChange" />
        <span>在新标签页打开（带 noopener,noreferrer）</span>
      </label>

      <p v-if="urlError" class="hint-error">{{ urlError }}</p>
    </div>

    <!-- 无动作 -->
    <div v-else class="action-block action-none">
      <p class="hint-warning">
        ⚠ 该按钮运行时不可点击：会渲染为禁用状态并提示「该按钮尚未配置动作」，允许保存。
      </p>
    </div>

    <p v-if="actionType !== 'none' && !typeError" class="action-preview">
      <span class="preview-label">动作预览：</span>
      <span class="preview-text">
        <template v-if="actionType === 'navigate'">
          {{
            navigateTargetPage
              ? `跳转到「${navigateTargetPage.name}」`
              : '尚未选择目标页面'
          }}
        </template>
        <template v-else>
          {{ externalUrl ? `新标签打开 ${externalUrl}` : '尚未填写链接' }}
        </template>
      </span>
    </p>
  </section>
</template>

<style scoped>
.action-panel {
  --page-danger-fg: var(--x-danger);
  --page-warning-fg: var(--x-warning);
  --page-success-fg: var(--x-success);
  padding: 16px;
  border-bottom: 1px solid var(--x-border);
  background: var(--x-canvas-soft);
}

.action-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.section-title {
  font-family: var(--x-mono);
  font-size: 9.5px;
  font-weight: 500;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.section-hint {
  margin-bottom: 14px;
  font-size: 10.5px;
  line-height: 1.6;
  color: var(--x-muted);
}

.action-block {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed var(--x-border);
}

.action-checkbox {
  margin-top: 6px;
  font-size: 12px;
}

.required {
  color: var(--page-danger-fg);
}

.field-meta {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.04em;
  color: var(--x-muted);
}

.hint-error {
  margin-top: 7px;
  font-size: 10.5px;
  line-height: 1.55;
  color: var(--page-danger-fg);
}

.hint-warning {
  margin-top: 7px;
  font-size: 10.5px;
  line-height: 1.55;
  color: var(--page-warning-fg);
}

.hint-ok {
  margin-top: 7px;
  font-size: 10.5px;
  line-height: 1.55;
  color: var(--page-success-fg);
}

.hint-muted {
  margin-top: 7px;
  font-size: 10.5px;
  line-height: 1.55;
  color: var(--x-muted);
}

.action-none {
  border-top: none;
  padding-top: 0;
}

.action-preview {
  margin-top: 14px;
  padding: 9px 11px;
  border: 1px dashed var(--x-border-strong);
  border-radius: var(--x-radius);
  font-size: 10.5px;
  line-height: 1.6;
  color: var(--x-muted);
}

.preview-label {
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.preview-text {
  color: var(--x-text-secondary);
  overflow-wrap: anywhere;
}

:global(.admin-theme-scope[data-admin-theme='light'] .action-panel) {
  --page-danger-fg: #b42318;
  --page-warning-fg: #8a5a00;
  --page-success-fg: #0f7a4e;
}
</style>
