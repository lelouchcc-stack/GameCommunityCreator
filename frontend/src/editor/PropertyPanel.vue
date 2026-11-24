<script setup lang="ts">
/**
 * 右栏属性面板（页面级编辑器）：
 * - 「组件属性」页签：选中组件 → 标题 / 显示隐藏 / 按 registry 的 propDefs 渲染控件；
 *   `button` 组件额外渲染「点击动作」配置（ActionPanel）；底部为只读信息（类型/数据源/ID/页面 ID）。
 * - 「页面设置」页签：页面标题、主题色板、字体、圆角（ThemePanel）+ 页面只读信息。
 *
 * 未选中组件时自动切到「页面设置」，但仍可从页签切回来查看提示。
 */
import { computed, ref, watch } from 'vue'
import { getComponentDef } from '@/components/registry'
import {
  type ComponentPropValue,
  type PageAction,
  type PageComponent,
  type PageConfig,
  type PropDef,
  type ThemeConfig,
} from '@/types/page'
import type { SitePageView } from '@/types/site'
import ActionPanel from './ActionPanel.vue'
import { dataSourceLabel } from './labels'
import ThemePanel from './ThemePanel.vue'

const props = defineProps<{
  config: PageConfig
  selected: PageComponent | null
  /** 站点全部页面（按钮动作目标） */
  pages: SitePageView[]
  /** 当前页面稳定 ID（只读信息展示） */
  pageId: string
}>()

const emit = defineEmits<{
  (event: 'update-page-title', value: string): void
  (event: 'update-theme', patch: Partial<ThemeConfig>): void
  (event: 'apply-preset', theme: ThemeConfig): void
  (event: 'update-component-title', value: string): void
  (event: 'update-prop', key: string, value: ComponentPropValue): void
  (event: 'toggle-visible', value: boolean): void
  (event: 'update-action', action: PageAction): void
}>()

const tab = ref<'component' | 'page'>('component')

watch(
  () => props.selected?.id ?? null,
  (id) => {
    tab.value = id ? 'component' : 'page'
  },
  { immediate: true },
)

/** 未选中组件时「组件属性」页签没有内容，强制显示页面设置 */
const effectiveTab = computed<'component' | 'page'>(() =>
  props.selected ? tab.value : 'page',
)

const def = computed(() => (props.selected ? getComponentDef(props.selected.type) : null))
const componentIndex = computed(() =>
  props.selected ? props.config.components.findIndex((item) => item.id === props.selected?.id) + 1 : 0,
)
const titleLength = computed(() => (props.selected?.title ?? '').length)
const titleEmpty = computed(() => (props.selected?.title ?? '').trim().length === 0)
const dataSourceLabelText = computed(() => {
  if (!props.selected) return ''
  return dataSourceLabel(props.selected.dataSource)
})
const supportsAction = computed(() => def.value?.supportsAction === true)

/* ---- 属性值读取（避免在模板里对 PropDef 联合类型做窄化） ---- */
function valueOf(key: string): ComponentPropValue | undefined {
  return props.selected?.props[key]
}

function textValueOf(key: string): string {
  const value = valueOf(key)
  if (value === undefined) return ''
  return typeof value === 'string' ? value : String(value)
}

function numberValueOf(key: string): number {
  const value = valueOf(key)
  return typeof value === 'number' ? value : 0
}

function boolValueOf(key: string): boolean {
  return valueOf(key) === true
}

function stringValueOf(key: string): string {
  const value = valueOf(key)
  return typeof value === 'string' ? value : ''
}

function textMaxLimit(prop: PropDef): number {
  return prop.type === 'text' || prop.type === 'textarea' ? prop.maxLength : 0
}

function placeholderFor(prop: PropDef): string {
  return prop.type === 'text' || prop.type === 'textarea' ? (prop.placeholder ?? '') : ''
}

function numberMin(prop: PropDef): number {
  return prop.type === 'number' ? prop.min : 0
}

function numberMax(prop: PropDef): number {
  return prop.type === 'number' ? prop.max : 0
}

function numberStep(prop: PropDef): number {
  return prop.type === 'number' ? (prop.step ?? 1) : 1
}

function selectOptions(prop: PropDef): ReadonlyArray<{ label: string; value: string }> {
  return prop.type === 'select' ? prop.options : []
}

/* ---- 事件 ---- */
function onTitleInput(event: Event): void {
  emit('update-component-title', (event.target as HTMLInputElement).value)
}

function onVisibleChange(event: Event): void {
  emit('toggle-visible', (event.target as HTMLInputElement).checked)
}

function onTextInput(key: string, event: Event): void {
  emit('update-prop', key, (event.target as HTMLInputElement | HTMLTextAreaElement).value)
}

function onNumberChange(key: string, min: number, max: number, event: Event): void {
  const input = event.target as HTMLInputElement
  const parsed = Number(input.value)
  if (!Number.isFinite(parsed)) {
    input.value = String(numberValueOf(key))
    return
  }
  const normalized = Math.round(Math.min(max, Math.max(min, parsed)))
  input.value = String(normalized)
  emit('update-prop', key, normalized)
}

function onBooleanChange(key: string, event: Event): void {
  emit('update-prop', key, (event.target as HTMLInputElement).checked)
}

function onSelectChange(key: string, event: Event): void {
  emit('update-prop', key, (event.target as HTMLSelectElement).value)
}

function onThemePatch(patch: Partial<ThemeConfig>): void {
  emit('update-theme', patch)
}

function onPresetApply(theme: ThemeConfig): void {
  emit('apply-preset', theme)
}

function onActionUpdate(action: PageAction): void {
  emit('update-action', action)
}

function switchTab(next: 'component' | 'page'): void {
  if (next === 'component' && !props.selected) return
  tab.value = next
}
</script>

<template>
  <div class="property-panel">
    <div class="panel-tabs" role="tablist">
      <button
        type="button"
        class="tab-btn"
        role="tab"
        :class="{ 'is-active': effectiveTab === 'component' }"
        :aria-selected="effectiveTab === 'component'"
        :disabled="!selected"
        :title="selected ? '编辑选中组件的属性' : '先在左侧或画布中选中一个组件'"
        @click="switchTab('component')"
      >
        组件属性
        <span v-if="selected" class="tab-index">{{ componentIndex }}/{{ config.components.length }}</span>
      </button>
      <button
        type="button"
        class="tab-btn"
        role="tab"
        :class="{ 'is-active': effectiveTab === 'page' }"
        :aria-selected="effectiveTab === 'page'"
        @click="switchTab('page')"
      >
        页面设置
      </button>
    </div>

    <!-- ================= 组件属性 ================= -->
    <template v-if="effectiveTab === 'component' && selected && def">
      <header class="panel-head">
        <div class="head-line">
          <h2 class="panel-title">组件属性</h2>
          <span class="gcc-badge">{{ def.label }}</span>
        </div>
        <span class="panel-sub">第 {{ componentIndex }} / {{ config.components.length }} 个组件</span>
      </header>

      <section class="panel-section">
        <div class="gcc-field">
          <label class="gcc-label" for="component-title-input">
            组件标题 <span class="required">*</span>
          </label>
          <input
            id="component-title-input"
            class="gcc-input"
            type="text"
            maxlength="60"
            :value="selected.title"
            placeholder="例如：最新公告"
            @input="onTitleInput"
          />
          <div class="field-meta">
            <span v-if="titleEmpty" class="is-warn">标题不能为空（保存时会校验）</span>
            <span v-else />
            <span :class="{ 'is-warn': titleLength > 60 }">{{ titleLength }}/60</span>
          </div>
        </div>

        <label class="gcc-checkbox visible-row">
          <input type="checkbox" :checked="selected.visible" @change="onVisibleChange" />
          <span>在页面中显示该组件</span>
        </label>
      </section>

      <section class="panel-section">
        <h3 class="section-title">组件设置</h3>
        <div class="prop-list">
          <template v-for="prop in def.propDefs" :key="prop.key">
            <!-- 单行文本 -->
            <div v-if="prop.type === 'text'" class="gcc-field">
              <label class="gcc-label" :for="`prop-${prop.key}`">{{ prop.label }}</label>
              <input
                :id="`prop-${prop.key}`"
                class="gcc-input"
                type="text"
                :maxlength="textMaxLimit(prop)"
                :placeholder="placeholderFor(prop)"
                :value="textValueOf(prop.key)"
                @input="onTextInput(prop.key, $event)"
              />
              <div class="field-meta">
                <span />
                <span>{{ textValueOf(prop.key).length }}/{{ textMaxLimit(prop) }}</span>
              </div>
            </div>

            <!-- 多行文本 -->
            <div v-else-if="prop.type === 'textarea'" class="gcc-field">
              <label class="gcc-label" :for="`prop-${prop.key}`">{{ prop.label }}</label>
              <textarea
                :id="`prop-${prop.key}`"
                class="gcc-textarea"
                :maxlength="textMaxLimit(prop)"
                :placeholder="placeholderFor(prop)"
                :value="textValueOf(prop.key)"
                @input="onTextInput(prop.key, $event)"
              />
              <div class="field-meta">
                <span>留空时由运行时回退到游戏信息</span>
                <span>{{ textValueOf(prop.key).length }}/{{ textMaxLimit(prop) }}</span>
              </div>
            </div>

            <!-- 数字 -->
            <div v-else-if="prop.type === 'number'" class="gcc-field">
              <label class="gcc-label" :for="`prop-${prop.key}`">{{ prop.label }}</label>
              <input
                :id="`prop-${prop.key}`"
                class="gcc-input"
                type="number"
                :min="numberMin(prop)"
                :max="numberMax(prop)"
                :step="numberStep(prop)"
                :value="numberValueOf(prop.key)"
                @change="onNumberChange(prop.key, numberMin(prop), numberMax(prop), $event)"
              />
              <div class="field-meta">
                <span>取值 {{ numberMin(prop) }} - {{ numberMax(prop) }}</span>
              </div>
            </div>

            <!-- 布尔 -->
            <label v-else-if="prop.type === 'boolean'" class="gcc-checkbox prop-checkbox">
              <input
                type="checkbox"
                :checked="boolValueOf(prop.key)"
                @change="onBooleanChange(prop.key, $event)"
              />
              <span>{{ prop.label }}</span>
            </label>

            <!-- 下拉 -->
            <div v-else class="gcc-field">
              <label class="gcc-label" :for="`prop-${prop.key}`">{{ prop.label }}</label>
              <select
                :id="`prop-${prop.key}`"
                class="gcc-select"
                :value="stringValueOf(prop.key)"
                @change="onSelectChange(prop.key, $event)"
              >
                <option v-for="option in selectOptions(prop)" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>
          </template>
        </div>
      </section>

      <!-- 按钮专用：点击动作配置（其他组件不渲染） -->
      <ActionPanel
        v-if="supportsAction"
        :component="selected"
        :pages="pages"
        @update-action="onActionUpdate"
      />

      <section class="panel-section">
        <h3 class="section-title">只读信息</h3>
        <dl class="meta-list">
          <div class="meta-row">
            <dt>组件类型</dt>
            <dd>{{ def.label }}（{{ selected.type }}）</dd>
          </div>
          <div class="meta-row">
            <dt>数据源</dt>
            <dd>{{ dataSourceLabelText }}</dd>
          </div>
          <div class="meta-row">
            <dt>组件 ID</dt>
            <dd class="gcc-mono">{{ selected.id }}</dd>
          </div>
          <div class="meta-row">
            <dt>页面 ID</dt>
            <dd class="gcc-mono">{{ pageId }}</dd>
          </div>
        </dl>
      </section>
    </template>

    <!-- ================= 页面设置 ================= -->
    <ThemePanel
      v-else
      :config="config"
      :page-id="pageId"
      @update-page-title="emit('update-page-title', $event)"
      @update-theme="onThemePatch"
      @apply-preset="onPresetApply"
    />
  </div>
</template>

<style scoped>
.property-panel {
  --page-danger-fg: var(--x-danger);
  min-height: 100%;
}

.panel-tabs {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  gap: 0;
  padding: 0 14px;
  border-bottom: 1px solid var(--x-border);
  background: var(--x-canvas-soft);
}

.tab-btn {
  position: relative;
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 13px 8px 11px;
  border: none;
  border-bottom: 1px solid transparent;
  border-radius: 0;
  background: transparent;
  color: var(--x-muted);
  font-size: 11.5px;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition:
    color 0.12s ease,
    border-color 0.12s ease;
}

.tab-btn:hover:not(:disabled) {
  color: var(--x-text);
}

.tab-btn.is-active {
  color: var(--x-text);
  border-bottom-color: var(--x-accent);
}

.tab-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.tab-index {
  font-family: var(--x-mono);
  font-size: 8.5px;
  letter-spacing: 0.08em;
  color: var(--x-muted);
}

.panel-head {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 16px 16px 14px;
  border-bottom: 1px solid var(--x-border);
}

.head-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-title {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: -0.005em;
}

.panel-sub {
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  color: var(--x-muted);
}

.panel-section {
  padding: 16px;
  border-bottom: 1px solid var(--x-border);
}

.panel-section:last-child {
  border-bottom: none;
}

.required {
  color: var(--x-danger);
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

.field-meta .is-warn {
  color: var(--page-danger-fg);
}

.field-help {
  margin-top: 6px;
  font-size: 10.5px;
  line-height: 1.6;
  color: var(--x-muted);
}

.visible-row {
  margin-top: 14px;
}

.section-title {
  margin-bottom: 12px;
  font-family: var(--x-mono);
  font-size: 9.5px;
  font-weight: 500;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.prop-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.prop-checkbox {
  padding: 2px 0;
}

.meta-list {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 11px;
}

.meta-row dt {
  flex: none;
  width: 66px;
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.meta-row dd {
  margin: 0;
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--x-text-secondary);
}

:global(.admin-theme-scope[data-admin-theme='light'] .property-panel) {
  --page-danger-fg: #b42318;
}
</style>
