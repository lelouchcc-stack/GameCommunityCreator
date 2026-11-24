<script setup lang="ts">
/**
 * Markdown 正文编辑器（v3.2）：编辑 / 预览 / 分栏三模式 + 工具栏 + 图片上传。
 *
 * 答辩前缺陷修复（P0）：
 * - 新增真实图片上传：选择本地图片 → POST /api/uploads/images → 在光标处插入
 *   `![图片描述](图片URL)`，不改变现有 Markdown 编辑模式；上传失败在编辑器内提示。
 * - 预览区链接点击拦截：避免在内容管理弹窗内整页跳转导致未保存内容丢失。
 * - 所有内部控件均为 type="button"，不会触发表单提交从而关闭父级弹窗。
 */
import { computed, nextTick, ref } from 'vue'
import MarkdownRenderer from './MarkdownRenderer.vue'
import { uploadImage, UploadError } from '@/api/uploads'

const props = withDefaults(
  defineProps<{
    modelValue: string
    label?: string
    placeholder?: string
    minHeight?: number
    required?: boolean
    maxLength?: number
    error?: string
    disabled?: boolean
    /** 是否显示「上传图片」按钮（内容管理等有真实上传接口的场景启用） */
    allowImageUpload?: boolean
  }>(),
  {
    label: '正文（Markdown）',
    placeholder: '支持 Markdown：标题、粗体、斜体、链接、图片、列表、引用、代码块、表格……',
    minHeight: 260,
    required: false,
    maxLength: 20000,
    error: '',
    disabled: false,
    allowImageUpload: true,
  },
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

type EditorMode = 'edit' | 'preview' | 'split'
const mode = ref<EditorMode>('edit')
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

const uploading = ref(false)
const uploadError = ref('')
const previewHint = ref('')
let hintTimer: number | undefined

const value = computed(() => props.modelValue ?? '')
const count = computed(() => value.value.length)

function update(valueNext: string): void {
  emit('update:modelValue', valueNext)
}

function onInput(event: Event): void {
  update((event.target as HTMLTextAreaElement).value)
}

function focusTextareaAt(cursor: number): void {
  void nextTick(() => {
    const textarea = textareaRef.value
    if (!textarea) return
    textarea.focus()
    textarea.setSelectionRange(cursor, cursor)
  })
}

/** 在光标处插入片段（图片 Markdown 用，保持不丢失已有内容） */
function insertAtCursor(fragment: string): void {
  const textarea = textareaRef.value
  const start = textarea?.selectionStart ?? value.value.length
  const end = textarea?.selectionEnd ?? start
  const next = `${value.value.slice(0, start)}${fragment}${value.value.slice(end)}`
  update(next)
  if (mode.value === 'preview') mode.value = 'split'
  focusTextareaAt(start + fragment.length)
}

function replaceSelection(prefix: string, suffix = '', placeholder = ''): void {
  const textarea = textareaRef.value
  if (!textarea || props.disabled) return
  const start = textarea.selectionStart ?? value.value.length
  const end = textarea.selectionEnd ?? start
  const selected = value.value.slice(start, end) || placeholder
  const next = `${value.value.slice(0, start)}${prefix}${selected}${suffix}${value.value.slice(end)}`
  update(next)
  focusTextareaAt(start + prefix.length + selected.length)
}

function insertBlock(block: string): void {
  const textarea = textareaRef.value
  if (!textarea || props.disabled) return
  const start = textarea.selectionStart ?? value.value.length
  const needsNewline = start > 0 && value.value[start - 1] !== '\n'
  const text = `${needsNewline ? '\n' : ''}${block}\n`
  update(`${value.value.slice(0, start)}${text}${value.value.slice(start)}`)
  focusTextareaAt(start + text.length)
}

/* ------------------------------ 图片上传 ------------------------------ */

function pickImage(): void {
  if (props.disabled || uploading.value) return
  uploadError.value = ''
  fileInputRef.value?.click()
}

function sanitizeAlt(name: string): string {
  const base = name.replace(/\.[^.]+$/, '').replace(/[[\]()]/g, '').trim().slice(0, 60)
  return base || '图片'
}

async function onFileChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // 允许重复选择同一文件
  if (!file) return
  uploadError.value = ''
  uploading.value = true
  try {
    const uploaded = await uploadImage(file)
    insertAtCursor(`![${sanitizeAlt(file.name)}](${uploaded.url})`)
  } catch (error) {
    uploadError.value =
      error instanceof UploadError ? error.message : '图片上传失败，请稍后重试'
  } finally {
    uploading.value = false
  }
}

/* ------------------------------ 预览链接拦截 ------------------------------ */

function onPreviewClick(event: MouseEvent): void {
  const target = event.target as HTMLElement | null
  const anchor = target?.closest?.('a')
  if (!anchor) return
  // 编辑弹窗内不跳转，避免页面被替换导致未保存内容丢失
  event.preventDefault()
  event.stopPropagation()
  previewHint.value = '预览中的链接不会跳转（避免离开编辑页丢失未保存内容），保存后可在公开页查看'
  window.clearTimeout(hintTimer)
  hintTimer = window.setTimeout(() => {
    previewHint.value = ''
  }, 3200)
}

/* ------------------------------ 工具栏 ------------------------------ */

interface ToolbarAction {
  key: string
  label: string
  title: string
  run: () => void
}

const toolbar = computed<ToolbarAction[]>(() => [
  { key: 'h2', label: 'H2', title: '标题', run: () => insertBlock('## 标题') },
  { key: 'bold', label: 'B', title: '粗体', run: () => replaceSelection('**', '**', '粗体文字') },
  { key: 'italic', label: 'I', title: '斜体', run: () => replaceSelection('*', '*', '斜体文字') },
  {
    key: 'link',
    label: '🔗',
    title: '链接',
    run: () => replaceSelection('[', '](https://example.com)', '链接文字'),
  },
  { key: 'ul', label: '• 列表', title: '无序列表', run: () => insertBlock('- 列表项一\n- 列表项二') },
  {
    key: 'ol',
    label: '1. 列表',
    title: '有序列表',
    run: () => insertBlock('1. 列表项一\n2. 列表项二'),
  },
  { key: 'quote', label: '❝', title: '引用', run: () => insertBlock('> 引用内容') },
  { key: 'code', label: '</>', title: '代码块', run: () => insertBlock('```\n代码内容\n```') },
  {
    key: 'table',
    label: '表格',
    title: '表格',
    run: () => insertBlock('| 列一 | 列二 |\n| --- | --- |\n| 内容 | 内容 |'),
  },
])
</script>

<template>
  <div class="markdown-editor" :class="{ 'is-invalid': Boolean(error), 'is-disabled': disabled }">
    <div class="markdown-editor__head">
      <label class="gcc-label">
        {{ label }}
        <em v-if="required" class="required">*</em>
      </label>
      <div class="markdown-editor__modes" role="group" aria-label="Markdown 编辑模式">
        <button
          v-for="item in [
            { key: 'edit', label: '编辑' },
            { key: 'preview', label: '预览' },
            { key: 'split', label: '分栏' },
          ]"
          :key="item.key"
          type="button"
          class="markdown-editor__mode"
          :class="{ 'is-active': mode === item.key }"
          @click="mode = item.key as EditorMode"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <div class="markdown-editor__toolbar" role="toolbar" aria-label="Markdown 工具栏">
      <button
        v-for="action in toolbar"
        :key="action.key"
        type="button"
        class="markdown-editor__tool"
        :title="action.title"
        :disabled="disabled"
        @click="action.run()"
      >
        {{ action.label }}
      </button>
      <template v-if="allowImageUpload">
        <button
          type="button"
          class="markdown-editor__tool markdown-editor__upload"
          :title="uploading ? '图片上传中…' : '上传图片（插入到当前光标位置）'"
          :disabled="disabled || uploading"
          @click="pickImage"
        >
          {{ uploading ? '上传中…' : '上传图片' }}
        </button>
        <input
          ref="fileInputRef"
          class="markdown-editor__file"
          type="file"
          accept="image/png,image/jpeg,image/webp,image/gif"
          tabindex="-1"
          aria-hidden="true"
          @change="onFileChange"
        />
      </template>
      <span class="markdown-editor__count gcc-muted">{{ count }}/{{ maxLength }}</span>
    </div>

    <p v-if="uploadError" class="markdown-editor__error" role="alert">{{ uploadError }}</p>

    <div class="markdown-editor__body" :class="`is-${mode}`">
      <textarea
        v-show="mode !== 'preview'"
        ref="textareaRef"
        class="gcc-textarea markdown-editor__textarea"
        :value="value"
        :placeholder="placeholder"
        :maxlength="maxLength"
        :disabled="disabled"
        :style="{ minHeight: `${minHeight}px` }"
        @input="onInput"
      ></textarea>
      <div
        v-show="mode !== 'edit'"
        class="markdown-editor__preview"
        :style="{ minHeight: `${minHeight}px` }"
        @click.capture="onPreviewClick"
      >
        <MarkdownRenderer
          :markdown="value"
          empty-text="（暂无正文，左侧输入 Markdown 后这里会显示预览）"
        />
      </div>
    </div>

    <p v-if="previewHint" class="markdown-editor__hint" role="status">{{ previewHint }}</p>
    <p v-if="error" class="markdown-editor__error">{{ error }}</p>
  </div>
</template>

<style scoped>
.markdown-editor {
  display: flex;
  flex-direction: column;
  gap: 6px;
  border: 1px solid var(--gcc-border);
  border-radius: 8px;
  padding: 10px 12px;
  background: var(--gcc-input-bg);
}

.markdown-editor.is-invalid {
  border-color: var(--gcc-danger);
}

.markdown-editor__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.required {
  color: var(--gcc-danger);
  font-style: normal;
}

.markdown-editor__modes {
  display: inline-flex;
  border: 1px solid var(--gcc-border);
  border-radius: var(--x-radius-pill);
  overflow: hidden;
}

.markdown-editor__mode {
  padding: 3px 10px;
  font-size: 12px;
  border: none;
  background: transparent;
  color: var(--gcc-muted);
  cursor: pointer;
}

.markdown-editor__mode.is-active {
  background: var(--gcc-hover-strong);
  color: var(--gcc-text);
}

.markdown-editor__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.markdown-editor__tool {
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid var(--gcc-border);
  background: transparent;
  color: var(--gcc-muted);
  font-size: 12px;
  cursor: pointer;
}

.markdown-editor__tool:hover:not(:disabled) {
  color: var(--gcc-text);
  border-color: var(--gcc-border-strong);
}

.markdown-editor__tool:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.markdown-editor__upload {
  margin-left: 4px;
  border-color: var(--gcc-border-strong);
  color: var(--gcc-text);
}

.markdown-editor__file {
  display: none;
}

.markdown-editor__count {
  margin-left: auto;
  font-size: 11px;
}

.markdown-editor__body {
  display: grid;
  gap: 10px;
}

.markdown-editor__body.is-split {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
}

.markdown-editor__textarea {
  width: 100%;
  border-radius: 8px;
  font-family: var(--x-mono);
  font-size: 13px;
  line-height: 1.7;
  resize: vertical;
}

.markdown-editor__preview {
  overflow: auto;
  max-height: 520px;
  padding: 8px 12px;
  border: 1px dashed var(--gcc-border-strong);
  border-radius: 8px;
  background: var(--gcc-surface);
}

.markdown-editor__error {
  font-size: 12px;
  color: var(--x-danger);
}

.markdown-editor__hint {
  font-size: 11.5px;
  color: var(--x-warning);
}

/* 管理端浅色主题适配 */
:global(.admin-theme-scope[data-admin-theme='light']) .markdown-editor {
  background: #f8faff;
}

:global(.admin-theme-scope[data-admin-theme='light']) .markdown-editor__preview {
  background: #ffffff;
}
</style>
