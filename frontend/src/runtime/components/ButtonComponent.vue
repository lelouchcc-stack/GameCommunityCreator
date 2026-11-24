<script setup lang="ts">
/**
 * button — 通用按钮（PAGE_SCHEMA §2.6，v2 重点）。
 *
 * 动作只经统一入口 executeAction 执行，禁止任何脚本/表达式求值：
 * - navigate     → 目标页面注册表解析后按模式跳转（编辑模式提示、预览跳预览路由、公开跳公开路由）
 * - externalLink → 校验 http/https 后安全新标签打开
 * - none/非法    → 渲染为禁用样式并给出提示
 *
 * 视觉：outline pill；主 CTA 反色（深色门户为白底黑字、浅色门户为黑底白字），颜色取自 --gcc-*。
 * 图标只是短文本（emoji/字符），不发起任何请求。
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { PageAction } from '@/types/page'
import type { RuntimeComponentProps } from '@/types/runtime'
import { isAllowedActionType } from '@/utils/actions'
import { executeAction } from '@/runtime/actions'
import { injectRuntimeMode } from '@/runtime/context'
import { readEnum, readString, readTrimmed } from '@/runtime/componentProps'

const props = defineProps<RuntimeComponentProps>()

const router = useRouter()
const mode = injectRuntimeMode()

const label = computed(() => readTrimmed(props.component, 'label', '按钮'))
const variant = computed(() =>
  readEnum(props.component, 'variant', ['primary', 'secondary', 'ghost'] as const, 'primary'),
)
const icon = computed(() => readString(props.component, 'icon').trim().slice(0, 8))
const align = computed(() =>
  readEnum(props.component, 'align', ['left', 'center', 'right'] as const, 'left'),
)

const action = computed<PageAction | undefined>(() => props.component.action)

/** 动作是否可用于点击：none / 未知类型 → 禁用并提示 */
const actionUsable = computed(() => {
  const current = action.value
  if (!current) return false
  const type = (current as { type?: unknown }).type
  if (!isAllowedActionType(type)) return false
  return type !== 'none'
})

const hint = computed(() => {
  const current = action.value
  if (!current) return '该按钮尚未配置动作'
  const type = (current as { type?: unknown }).type
  if (!isAllowedActionType(type)) return '暂不支持的动作类型'
  return type === 'none' ? '该按钮尚未配置动作' : ''
})

/** 预览 / 公开模式下的「动作目标」说明，便于编辑器外一眼看出按钮会去向 */
const actionSummary = computed(() => {
  const current = action.value
  if (!current) return ''
  if (current.type === 'navigate') {
    const target = props.site.pages.find((page) => page.pageId === current.targetPageId)
    return target ? `跳转：${target.name}` : '目标页面不存在或尚未发布'
  }
  if (current.type === 'externalLink') return current.url
  return ''
})

const clickable = computed(() => actionUsable.value)

function onClick(): void {
  if (!clickable.value) return
  executeAction(action.value ?? null, {
    router,
    site: props.site,
    mode: mode.value,
  })
}
</script>

<template>
  <section
    class="runtime-button"
    :aria-label="component.title"
    :data-action-type="action?.type ?? 'none'"
  >
    <div class="runtime-button__inner gcc-container" :class="`runtime-button__inner--${align}`">
      <button
        type="button"
        class="gcc-btn runtime-button__control"
        :class="[`gcc-btn-${variant}`, { 'runtime-button__control--disabled': !clickable }]"
        :disabled="!clickable"
        :aria-disabled="!clickable"
        :title="hint || actionSummary"
        :data-component-action="action?.type ?? 'none'"
        @click="onClick"
      >
        <span v-if="icon" class="runtime-button__icon" aria-hidden="true">{{ icon }}</span>
        <span class="runtime-button__label">{{ label }}</span>
        <span v-if="clickable" class="runtime-button__arrow" aria-hidden="true">→</span>
      </button>
      <p v-if="!clickable && hint" class="runtime-button__hint">{{ hint }}</p>
    </div>
  </section>
</template>

<style scoped>
.runtime-button {
  padding: 40px 0 8px;
}

.runtime-button__inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.runtime-button__inner--left {
  justify-content: flex-start;
}

.runtime-button__inner--center {
  justify-content: center;
}

.runtime-button__inner--right {
  justify-content: flex-end;
}

/* 用门户主题变量覆盖全局 pill 按钮颜色（全局 .gcc-btn 使用管理端 --x-* 令牌） */
.runtime-button__control.gcc-btn {
  padding: 11px 26px;
  font-size: 14px;
  border-color: var(--gcc-border-strong);
  background: transparent;
  color: var(--gcc-text);
}

.runtime-button__control.gcc-btn:hover:not(:disabled) {
  border-color: var(--gcc-text);
  background: var(--gcc-hover);
  color: var(--gcc-text);
}

.runtime-button__control.gcc-btn-primary {
  border-color: var(--gcc-text);
  background: var(--gcc-text);
  color: var(--gcc-bg);
}

.runtime-button__control.gcc-btn-primary:hover:not(:disabled) {
  border-color: var(--gcc-text);
  background: var(--gcc-text);
  color: var(--gcc-bg);
  opacity: 0.9;
}

.runtime-button__control.gcc-btn-ghost {
  border-color: transparent;
  color: var(--gcc-muted);
}

.runtime-button__control.gcc-btn-ghost:hover:not(:disabled) {
  color: var(--gcc-text);
  background: var(--gcc-hover);
}

.runtime-button__control--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.runtime-button__icon {
  font-size: 14px;
  line-height: 1;
}

.runtime-button__arrow {
  font-size: 13px;
  line-height: 1;
}

.runtime-button__hint {
  flex: none;
  font-family: var(--gcc-mono);
  font-size: 10.5px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}
</style>
