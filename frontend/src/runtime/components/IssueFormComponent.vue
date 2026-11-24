<script setup lang="ts">
/**
 * issue_form — 问题汇报表单（PAGE_SCHEMA §2.10）。
 *
 * 字段固定（title / category / description / gameVersion / steps），提交固定调用
 * `POST /api/public/communities/{slug}/issues`，不接受配置中的任意请求定义。
 *
 * - 公开模式：真实提交，成功后按 successMessage 展示成功提示（默认含「感谢反馈」）；
 * - 编辑器 / 预览模式：禁用提交按钮并提示「预览模式不可提交」，避免草稿环境写库。
 *
 * 视觉：深色 hairline 面板 + mono 大写标签 + pill 提交按钮（颜色取自 --gcc-*）。
 */
import { computed, reactive, ref } from 'vue'
import { submitIssue } from '@/api/public'
import type { RuntimeComponentProps } from '@/types/runtime'
import { toErrorMessage } from '@/runtime/errors'
import { injectRuntimeMode } from '@/runtime/context'
import { readString, readTrimmed } from '@/runtime/componentProps'
import { pushToast } from '@/runtime/toast'
import SectionHead from './SectionHead.vue'

const props = defineProps<RuntimeComponentProps>()

const mode = injectRuntimeMode()

const DEFAULT_CATEGORIES = ['Bug 反馈', '体验建议', '其他']
const TITLE_MAX = 120
const CATEGORY_MAX = 40
const DESCRIPTION_MAX = 3000
const GAME_VERSION_MAX = 40
const STEPS_MAX = 2000

const intro = computed(() => readString(props.component, 'intro').trim())
const successMessage = computed(() =>
  readTrimmed(props.component, 'successMessage', '感谢反馈！我们会尽快处理。'),
)

const categories = computed<string[]>(() => {
  const raw = readString(props.component, 'categories')
  const parsed = raw
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0 && item.length <= 20)
    .slice(0, 5)
  return parsed.length > 0 ? parsed : DEFAULT_CATEGORIES
})

const form = reactive({
  title: '',
  category: categories.value[0] ?? 'Bug 反馈',
  description: '',
  gameVersion: '',
  steps: '',
})

const fieldErrors = reactive<Record<string, string>>({})
const submitting = ref(false)
const submitted = ref(false)
const submitError = ref('')

const canSubmit = computed(() => mode.value === 'public')
const submitHint = computed(() => (canSubmit.value ? '' : '预览模式不可提交，请发布后在公开站点反馈'))

function validate(): boolean {
  for (const key of Object.keys(fieldErrors)) delete fieldErrors[key]
  const title = form.title.trim()
  if (!title) fieldErrors.title = '请填写问题标题'
  else if (title.length > TITLE_MAX) fieldErrors.title = `标题不能超过 ${TITLE_MAX} 个字符`

  const category = form.category.trim()
  if (!category) fieldErrors.category = '请选择问题类别'
  else if (category.length > CATEGORY_MAX) fieldErrors.category = `类别不能超过 ${CATEGORY_MAX} 个字符`

  const description = form.description.trim()
  if (!description) fieldErrors.description = '请描述你遇到的问题'
  else if (description.length > DESCRIPTION_MAX)
    fieldErrors.description = `描述不能超过 ${DESCRIPTION_MAX} 个字符`

  if (form.gameVersion.trim().length > GAME_VERSION_MAX)
    fieldErrors.gameVersion = `游戏版本不能超过 ${GAME_VERSION_MAX} 个字符`
  if (form.steps.trim().length > STEPS_MAX)
    fieldErrors.steps = `复现步骤不能超过 ${STEPS_MAX} 个字符`

  return Object.keys(fieldErrors).length === 0
}

async function onSubmit(): Promise<void> {
  if (!canSubmit.value) {
    pushToast('预览模式不可提交，请发布后在公开站点反馈')
    return
  }
  if (submitting.value) return
  if (!props.site.projectSlug) {
    pushToast('站点标识缺失，暂时无法提交', 'error')
    return
  }
  submitError.value = ''
  submitted.value = false
  if (!validate()) return

  submitting.value = true
  try {
    await submitIssue(props.site.projectSlug, {
      title: form.title.trim(),
      category: form.category.trim(),
      description: form.description.trim(),
      gameVersion: form.gameVersion.trim(),
      steps: form.steps.trim(),
    })
    submitted.value = true
    pushToast(successMessage.value, 'success')
    form.title = ''
    form.description = ''
    form.gameVersion = ''
    form.steps = ''
  } catch (error) {
    submitError.value = toErrorMessage(error, '提交失败，请稍后重试')
    pushToast(submitError.value, 'error')
  } finally {
    submitting.value = false
  }
}

function onReset(): void {
  for (const key of Object.keys(fieldErrors)) delete fieldErrors[key]
  submitError.value = ''
  submitted.value = false
  form.title = ''
  form.description = ''
  form.gameVersion = ''
  form.steps = ''
  form.category = categories.value[0] ?? 'Bug 反馈'
}
</script>

<template>
  <section class="runtime-issue" :aria-label="component.title">
    <div class="gcc-container">
      <SectionHead :title="component.title" kicker="FEEDBACK / BUG REPORTS" />

      <div class="issue-card">
        <p v-if="intro" class="issue-intro">{{ intro }}</p>

        <p v-if="submitted" class="issue-success" role="status">
          {{ successMessage }}
        </p>
        <p v-if="submitHint" class="issue-hint" role="note">{{ submitHint }}</p>
        <p v-if="submitError" class="issue-error" role="alert">{{ submitError }}</p>

        <form class="issue-form" novalidate @submit.prevent="onSubmit">
          <div class="gcc-field issue-form__field issue-form__field--full">
            <label class="gcc-label" :for="`issue-title-${component.id}`">
              问题标题 <span class="issue-required">*</span>
            </label>
            <input
              :id="`issue-title-${component.id}`"
              v-model="form.title"
              class="gcc-input"
              type="text"
              maxlength="120"
              placeholder="一句话概括问题，例如：启动后黑屏"
              :aria-invalid="Boolean(fieldErrors.title)"
            />
            <p v-if="fieldErrors.title" class="issue-field-error">{{ fieldErrors.title }}</p>
          </div>

          <div class="gcc-field issue-form__field">
            <label class="gcc-label" :for="`issue-category-${component.id}`">
              问题类别 <span class="issue-required">*</span>
            </label>
            <select
              :id="`issue-category-${component.id}`"
              v-model="form.category"
              class="gcc-select"
              :aria-invalid="Boolean(fieldErrors.category)"
            >
              <option v-for="category in categories" :key="category" :value="category">
                {{ category }}
              </option>
            </select>
            <p v-if="fieldErrors.category" class="issue-field-error">{{ fieldErrors.category }}</p>
          </div>

          <div class="gcc-field issue-form__field">
            <label class="gcc-label" :for="`issue-version-${component.id}`">游戏版本</label>
            <input
              :id="`issue-version-${component.id}`"
              v-model="form.gameVersion"
              class="gcc-input"
              type="text"
              maxlength="40"
              placeholder="例如：v0.9.2（可留空）"
            />
            <p v-if="fieldErrors.gameVersion" class="issue-field-error">{{ fieldErrors.gameVersion }}</p>
          </div>

          <div class="gcc-field issue-form__field issue-form__field--full">
            <label class="gcc-label" :for="`issue-desc-${component.id}`">
              问题描述 <span class="issue-required">*</span>
            </label>
            <textarea
              :id="`issue-desc-${component.id}`"
              v-model="form.description"
              class="gcc-textarea"
              maxlength="3000"
              placeholder="发生了什么？你期望的结果是什么？"
              :aria-invalid="Boolean(fieldErrors.description)"
            ></textarea>
            <p v-if="fieldErrors.description" class="issue-field-error">
              {{ fieldErrors.description }}
            </p>
          </div>

          <div class="gcc-field issue-form__field issue-form__field--full">
            <label class="gcc-label" :for="`issue-steps-${component.id}`">复现步骤</label>
            <textarea
              :id="`issue-steps-${component.id}`"
              v-model="form.steps"
              class="gcc-textarea"
              maxlength="2000"
              placeholder="1. 打开游戏&#10;2. 点击开始&#10;3. ……"
            ></textarea>
            <p v-if="fieldErrors.steps" class="issue-field-error">{{ fieldErrors.steps }}</p>
          </div>

          <div class="issue-form__actions issue-form__field--full">
            <button
              class="gcc-btn gcc-btn-primary issue-form__submit"
              type="submit"
              :disabled="!canSubmit || submitting"
            >
              {{ submitting ? '提交中…' : '提交反馈' }}
            </button>
            <button
              class="gcc-btn gcc-btn-ghost issue-form__reset"
              type="button"
              :disabled="submitting"
              @click="onReset"
            >
              重置
            </button>
            <span v-if="!canSubmit" class="issue-disabled-note">预览模式不可提交</span>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>

<style scoped>
.runtime-issue {
  padding: 64px 0 8px;
}

.issue-card {
  padding: 28px;
  border-radius: var(--gcc-card-radius, 8px);
  border: 1px solid var(--gcc-border);
  background: var(--gcc-card-bg);
}

.issue-intro {
  max-width: 720px;
  margin-bottom: 20px;
  color: var(--gcc-muted);
  font-size: 13.5px;
  line-height: 1.8;
}

.issue-hint {
  margin-bottom: 16px;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--gcc-border-strong);
  color: var(--gcc-muted);
  font-family: var(--gcc-mono);
  font-size: 10.5px;
  letter-spacing: 0.06em;
}

.issue-success {
  margin-bottom: 16px;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid rgba(61, 220, 151, 0.4);
  background: rgba(61, 220, 151, 0.08);
  color: var(--gcc-success, #3ddc97);
  font-size: 13.5px;
}

.issue-error {
  margin-bottom: 16px;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid rgba(242, 95, 92, 0.4);
  background: rgba(242, 95, 92, 0.08);
  color: var(--gcc-danger, #f25f5c);
  font-size: 13.5px;
}

.issue-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.issue-form__field--full {
  grid-column: 1 / -1;
}

.issue-required {
  color: var(--gcc-danger, #f25f5c);
}

.issue-form :deep(.gcc-input),
.issue-form :deep(.gcc-textarea),
.issue-form :deep(.gcc-select) {
  background: var(--gcc-input-bg);
  border-color: var(--gcc-border);
  color: var(--gcc-text);
  border-radius: 8px;
}

.issue-form :deep(.gcc-input:focus),
.issue-form :deep(.gcc-textarea:focus),
.issue-form :deep(.gcc-select:focus) {
  border-color: var(--gcc-border-strong);
}

.issue-form :deep(.gcc-textarea) {
  min-height: 110px;
}

.issue-field-error {
  color: var(--gcc-danger, #f25f5c);
  font-size: 12px;
}

.issue-form__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 4px;
}

/* 门户主题下覆盖全局 pill 按钮颜色 */
.issue-form__submit.gcc-btn.gcc-btn-primary {
  padding: 10px 26px;
  border-color: var(--gcc-text);
  background: var(--gcc-text);
  color: var(--gcc-bg);
  font-size: 13.5px;
}

.issue-form__submit.gcc-btn.gcc-btn-primary:hover:not(:disabled) {
  border-color: var(--gcc-text);
  background: var(--gcc-text);
  color: var(--gcc-bg);
  opacity: 0.9;
}

.issue-form__reset.gcc-btn.gcc-btn-ghost {
  border-color: var(--gcc-border-strong);
  color: var(--gcc-muted);
  font-size: 13px;
}

.issue-form__reset.gcc-btn.gcc-btn-ghost:hover:not(:disabled) {
  color: var(--gcc-text);
  border-color: var(--gcc-border-strong);
  background: var(--gcc-hover);
}

.issue-disabled-note {
  font-family: var(--gcc-mono);
  font-size: 10.5px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

@media (max-width: 720px) {
  .issue-form {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
