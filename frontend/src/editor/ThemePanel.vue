<script setup lang="ts">
/**
 * 右栏（未选中组件时）：页面设置 —— 页面标题、主题（颜色/预设/字体/圆角）、数据源（只读）。
 * 全部修改直接写回编辑器本地配置，画布通过 CSS 变量即时生效。
 */
import { computed } from 'vue'
import {
  DATA_SOURCES,
  FONT_FAMILIES,
  THEME_PRESETS,
  type FontFamily,
  type PageConfig,
  type ThemeConfig,
} from '@/types/page'
import { COLOR_PATTERN, matchThemePreset } from '@/utils/pageConfig'
import { dataSourceApi, dataSourceLabel } from './labels'

type ColorKey =
  | 'primaryColor'
  | 'accentColor'
  | 'backgroundColor'
  | 'surfaceColor'
  | 'textColor'
  | 'mutedTextColor'

const props = defineProps<{
  config: PageConfig
  /** 当前页面稳定 ID（只读信息展示） */
  pageId: string
}>()

const emit = defineEmits<{
  (event: 'update-page-title', value: string): void
  (event: 'update-theme', patch: Partial<ThemeConfig>): void
  (event: 'apply-preset', theme: ThemeConfig): void
}>()

const colorFields: ReadonlyArray<{ key: ColorKey; label: string; hint: string }> = [
  { key: 'primaryColor', label: '主色', hint: '按钮、链接、选中态' },
  { key: 'accentColor', label: '强调色', hint: '渐变另一端' },
  { key: 'backgroundColor', label: '页面背景', hint: '画布与公开页底色' },
  { key: 'surfaceColor', label: '卡片表面', hint: '区块/卡片底色' },
  { key: 'textColor', label: '正文颜色', hint: '主要文字' },
  { key: 'mutedTextColor', label: '次要文字', hint: '说明与辅助信息' },
]

const fontOptions: ReadonlyArray<{ value: FontFamily; label: string }> = [
  { value: 'system', label: '系统默认（黑体）' },
  { value: 'serif', label: '衬线（宋体 / Georgia）' },
  { value: 'mono', label: '等宽（代码风）' },
]

const pageTitleLength = computed(() => props.config.pageTitle.length)

const activePresetId = computed(() => matchThemePreset(props.config.theme))

const dataSourceRows = computed(() =>
  DATA_SOURCES.map((id) => ({
    id,
    label: dataSourceLabel(id),
    api: dataSourceApi(id),
    usedBy: props.config.components
      .filter((component) => component.dataSource === id)
      .map((component) => component.title),
  })),
)

const staticComponents = computed(() =>
  props.config.components.filter((component) => component.dataSource === null),
)

function onPageTitleInput(event: Event): void {
  emit('update-page-title', (event.target as HTMLInputElement).value)
}

function emitTheme(patch: Partial<ThemeConfig>): void {
  emit('update-theme', patch)
}

function onColorPicker(key: ColorKey, event: Event): void {
  emitTheme({ [key]: (event.target as HTMLInputElement).value } as Partial<ThemeConfig>)
}

function onHexChange(key: ColorKey, event: Event): void {
  const input = event.target as HTMLInputElement
  const value = input.value.trim()
  if (COLOR_PATTERN.test(value)) {
    emitTheme({ [key]: value.toLowerCase() } as Partial<ThemeConfig>)
  } else {
    // 非法输入直接回退到当前值，避免把坏颜色写进配置
    input.value = props.config.theme[key]
  }
}

function onFontChange(event: Event): void {
  const value = (event.target as HTMLSelectElement).value as FontFamily
  if (FONT_FAMILIES.includes(value)) emitTheme({ fontFamily: value })
}

function onRadiusInput(event: Event): void {
  const input = event.target as HTMLInputElement
  const parsed = Number(input.value)
  if (!Number.isFinite(parsed)) return
  emitTheme({ borderRadius: Math.round(Math.min(24, Math.max(0, parsed))) })
}

function applyPreset(theme: ThemeConfig): void {
  emit('apply-preset', { ...theme })
}
</script>

<template>
  <div class="theme-panel">
    <header class="panel-head">
      <h2 class="panel-title">页面设置</h2>
      <span class="panel-sub">未选中组件</span>
    </header>

    <section class="panel-section">
      <label class="gcc-label" for="page-title-input">页面标题</label>
      <input
        id="page-title-input"
        class="gcc-input"
        type="text"
        maxlength="60"
        :value="config.pageTitle"
        placeholder="例如：星汐边境 官方社区"
        @input="onPageTitleInput"
      />
      <div class="field-meta">
        <span>显示在页面顶部 / 浏览器标签</span>
        <span :class="{ 'is-warn': pageTitleLength > 60 }">{{ pageTitleLength }}/60</span>
      </div>
    </section>

    <section class="panel-section">
      <h3 class="section-title">主题色板</h3>
      <div class="preset-grid">
        <button
          v-for="preset in THEME_PRESETS"
          :key="preset.id"
          type="button"
          class="preset-card"
          :class="{ 'is-active': activePresetId === preset.id }"
          :title="`应用主题预设：${preset.label}`"
          @click="applyPreset(preset.theme)"
        >
          <span class="preset-swatches" aria-hidden="true">
            <i :style="{ background: preset.theme.primaryColor }" />
            <i :style="{ background: preset.theme.accentColor }" />
            <i :style="{ background: preset.theme.backgroundColor }" />
            <i :style="{ background: preset.theme.surfaceColor }" />
          </span>
          <span class="preset-label">{{ preset.label }}</span>
        </button>
      </div>
    </section>

    <section class="panel-section">
      <h3 class="section-title">颜色</h3>
      <div class="color-list">
        <div v-for="field in colorFields" :key="field.key" class="color-row">
          <span class="color-name">
            {{ field.label }}
            <span class="color-hint">{{ field.hint }}</span>
          </span>
          <span class="color-controls">
            <input
              class="gcc-color-input"
              type="color"
              :value="config.theme[field.key]"
              :aria-label="`${field.label}颜色选择器`"
              @input="onColorPicker(field.key, $event)"
            />
            <input
              class="hex-input gcc-mono"
              type="text"
              maxlength="7"
              :value="config.theme[field.key]"
              :aria-label="`${field.label}颜色十六进制值`"
              @change="onHexChange(field.key, $event)"
            />
          </span>
        </div>
      </div>
    </section>

    <section class="panel-section">
      <h3 class="section-title">排版</h3>
      <div class="field-block">
        <label class="gcc-label" for="theme-font">字体</label>
        <select id="theme-font" class="gcc-select" :value="config.theme.fontFamily" @change="onFontChange">
          <option v-for="option in fontOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>
      <div class="field-block">
        <label class="gcc-label" for="theme-radius">圆角 <span class="radius-value">{{ config.theme.borderRadius }}px</span></label>
        <div class="radius-row">
          <input
            id="theme-radius"
            type="range"
            min="0"
            max="24"
            step="1"
            :value="config.theme.borderRadius"
            @input="onRadiusInput"
          />
          <input
            class="radius-number"
            type="number"
            min="0"
            max="24"
            step="1"
            :value="config.theme.borderRadius"
            @change="onRadiusInput"
          />
        </div>
      </div>
    </section>

    <section class="panel-section">
      <h3 class="section-title">数据源 <span class="readonly-tag">只读</span></h3>
      <p class="section-hint">数据源由组件类型固定，运行时按此调用对应接口；预览读管理端接口，公开页读已发布快照。</p>
      <ul class="source-list">
        <li v-for="row in dataSourceRows" :key="row.id" class="source-item">
          <span class="source-name">{{ row.label }}</span>
          <span class="source-count">{{ row.usedBy.length }} 个组件</span>
          <span class="source-api gcc-mono">{{ row.api }}</span>
          <span v-if="row.usedBy.length" class="source-users">
            使用中：{{ row.usedBy.join('、') }}
          </span>
          <span v-else class="source-users is-muted">当前页面未使用</span>
        </li>
        <li v-if="staticComponents.length" class="source-item">
          <span class="source-name">无数据源</span>
          <span class="source-count">{{ staticComponents.length }} 个组件</span>
          <span class="source-api gcc-mono">不发起请求（静态组件）</span>
          <span class="source-users">使用中：{{ staticComponents.map((item) => item.title).join('、') }}</span>
        </li>
      </ul>
    </section>

    <section class="panel-section">
      <h3 class="section-title">只读信息</h3>
      <dl class="meta-list">
        <div class="meta-row">
          <dt>页面 ID</dt>
          <dd class="gcc-mono">{{ pageId }}</dd>
        </div>
        <div class="meta-row">
          <dt>项目标识</dt>
          <dd class="gcc-mono">{{ config.projectSlug }}</dd>
        </div>
        <div class="meta-row">
          <dt>配置版本</dt>
          <dd>schemaVersion {{ config.schemaVersion }} · {{ config.components.length }} 个组件</dd>
        </div>
      </dl>
    </section>
  </div>
</template>

<style scoped>
.theme-panel {
  --page-danger-fg: var(--x-danger);
  padding: 16px 16px 32px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--x-border);
}

.panel-title {
  font-size: 14px;
  font-weight: 500;
}

.panel-sub {
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  color: var(--x-muted);
}

.panel-section {
  padding: 16px 0;
  border-top: 1px solid var(--x-border);
}

.panel-section:first-of-type {
  padding-top: 0;
  border-top: none;
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

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-family: var(--x-mono);
  font-size: 9.5px;
  font-weight: 500;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.readonly-tag {
  padding: 1px 6px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-pill);
  font-family: var(--x-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
  color: var(--x-muted);
}

.section-hint {
  margin-bottom: 10px;
  font-size: 10.5px;
  line-height: 1.6;
  color: var(--x-muted);
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.preset-card {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 7px;
  padding: 8px;
  border: 1px solid var(--x-border);
  border-radius: 6px;
  background: transparent;
  color: var(--x-text);
  cursor: pointer;
  transition:
    border-color 0.12s ease,
    background 0.12s ease;
}

.preset-card:hover {
  border-color: var(--x-border-strong);
  background: var(--x-surface-2);
}

.preset-card.is-active {
  border-color: var(--x-accent);
  background: var(--x-accent-soft);
}

.preset-swatches {
  display: inline-flex;
  gap: 2px;
  height: 14px;
  border-radius: 3px;
  overflow: hidden;
}

.preset-swatches i {
  flex: 1;
  height: 100%;
}

.preset-label {
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  text-align: center;
  color: var(--x-muted);
}

.preset-card.is-active .preset-label {
  color: var(--x-text);
}

.color-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.color-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.color-name {
  display: flex;
  flex-direction: column;
  font-size: 11.5px;
  line-height: 1.35;
}

.color-hint {
  font-size: 9.5px;
  color: var(--x-muted);
}

.color-controls {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.hex-input {
  width: 84px;
  padding: 5px 6px;
  border-radius: var(--x-radius);
  border: 1px solid var(--x-border);
  background: var(--gcc-input-bg);
  color: var(--x-text);
  font-size: 10px;
  letter-spacing: 0.04em;
  text-align: center;
  outline: none;
}

.hex-input:focus {
  border-color: var(--x-border-strong);
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-bottom: 14px;
}

.field-block:last-child {
  margin-bottom: 0;
}

.radius-value {
  margin-left: 6px;
  font-family: var(--x-mono);
  color: var(--x-text-secondary);
}

.radius-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.radius-row input[type='range'] {
  flex: 1;
  accent-color: var(--x-accent);
}

.radius-number {
  width: 60px;
  padding: 5px 8px;
  border-radius: var(--x-radius);
  border: 1px solid var(--x-border);
  background: var(--gcc-input-bg);
  color: var(--x-text);
  font-size: 10.5px;
  text-align: center;
  outline: none;
}

.radius-number:focus {
  border-color: var(--x-border-strong);
}

.source-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.source-item {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px;
  padding: 9px 0;
  border-top: 1px solid var(--x-border);
}

.source-item:first-child {
  border-top: none;
  padding-top: 2px;
}

.source-name {
  font-size: 11.5px;
  font-weight: 500;
}

.source-count {
  padding: 0 6px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-pill);
  font-family: var(--x-mono);
  font-size: 8.5px;
  letter-spacing: 0.06em;
  color: var(--x-muted);
}

.source-api {
  width: 100%;
  font-size: 9px;
  letter-spacing: 0.03em;
  color: var(--x-muted);
  overflow-wrap: anywhere;
}

.source-users {
  width: 100%;
  font-size: 10px;
  color: var(--x-text-secondary);
}

.source-users.is-muted {
  color: var(--x-muted);
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

:global(.admin-theme-scope[data-admin-theme='light'] .theme-panel) {
  --page-danger-fg: #b42318;
}
</style>
