<script setup lang="ts">
/**
 * 编辑站点信息弹窗（站点名称 / 游戏名称 / 简介）。
 *
 * 管理端「总览」与「我的游戏」共用：v4.3 修复总览页「编辑站点信息」按钮
 * 点击无响应的问题时抽出，保证两处的字段校验、错误提示与保存行为完全一致。
 * 保存成功后抛出 saved，由页面关闭弹窗并刷新列表。
 */
import { reactive, ref } from 'vue'
import { isApiError } from '@/api/client'
import { updateProject } from '@/api/projects'
import { errorMessageOf } from '@/editor/useEditorState'
import { useToast } from '@/editor/useToast'
import type { ProjectView } from '@/types/api'

const props = defineProps<{ project: ProjectView }>()

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'saved', project: ProjectView): void
}>()

const toast = useToast()

const saving = ref(false)
const error = ref('')
const fieldErrors = ref<Record<string, string>>({})

const form = reactive<{ name: string; gameName: string; description: string }>({
  name: props.project.name,
  gameName: props.project.gameName,
  description: props.project.description ?? '',
})

function close(): void {
  if (saving.value) return
  emit('close')
}

function validate(): boolean {
  const errors: Record<string, string> = {}
  const name = form.name.trim()
  const gameName = form.gameName.trim()
  if (name.length < 1) errors.name = '请填写站点名称'
  else if (name.length > 80) errors.name = '站点名称不能超过 80 个字符'
  if (gameName.length < 1) errors.gameName = '请填写游戏名称'
  else if (gameName.length > 80) errors.gameName = '游戏名称不能超过 80 个字符'
  if (form.description.length > 500) errors.description = '简介不能超过 500 个字符'
  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

async function submit(): Promise<void> {
  if (saving.value) return
  error.value = ''
  if (!validate()) return
  const name = form.name.trim()
  saving.value = true
  try {
    const updated = await updateProject(props.project.id, {
      name,
      gameName: form.gameName.trim(),
      description: form.description.trim(),
    })
    toast.success(`站点「${name}」信息已更新`)
    emit('saved', updated)
  } catch (err) {
    if (isApiError(err)) {
      fieldErrors.value = { ...(err.fieldErrors ?? {}) }
      if (Object.keys(fieldErrors.value).length === 0) {
        error.value = err.message || '保存失败，请检查填写内容'
      }
      toast.error(err.message || '保存失败')
    } else {
      error.value = errorMessageOf(err, '保存失败，请稍后重试')
      toast.error(error.value)
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="x-mask" @click.self="close">
    <section class="dialog" role="dialog" aria-modal="true" aria-label="编辑站点信息">
      <header class="dialog__head">
        <p class="x-kicker">EDIT PROJECT</p>
        <h2 class="dialog__title">编辑站点信息</h2>
      </header>
      <form class="dialog__body" @submit.prevent="submit">
        <div class="gcc-field">
          <label class="gcc-label" for="edit-project-name">站点名称</label>
          <input id="edit-project-name" v-model="form.name" class="gcc-input" maxlength="80" />
          <p v-if="fieldErrors.name" class="dialog__field-error">{{ fieldErrors.name }}</p>
        </div>
        <div class="gcc-field">
          <label class="gcc-label" for="edit-project-game">游戏名称</label>
          <input
            id="edit-project-game"
            v-model="form.gameName"
            class="gcc-input"
            maxlength="80"
          />
          <p v-if="fieldErrors.gameName" class="dialog__field-error">
            {{ fieldErrors.gameName }}
          </p>
        </div>
        <div class="gcc-field">
          <label class="gcc-label" for="edit-project-desc">简介</label>
          <textarea
            id="edit-project-desc"
            v-model="form.description"
            class="gcc-textarea"
            maxlength="500"
          ></textarea>
          <p v-if="fieldErrors.description" class="dialog__field-error">
            {{ fieldErrors.description }}
          </p>
        </div>
        <p class="dialog__meta x-mono">
          SLUG /{{ project.slug }} · 修改名称不会改变公开链接
        </p>
        <p v-if="error" class="dialog__error">{{ error }}</p>
        <footer class="dialog__footer">
          <button type="button" class="gcc-btn gcc-btn-ghost" :disabled="saving" @click="close">
            取消
          </button>
          <button type="submit" class="gcc-btn gcc-btn-primary" :disabled="saving">
            {{ saving ? '保存中…' : '保存修改' }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>

<style scoped>
.dialog {
  width: min(560px, 100%);
  border: 1px solid var(--x-border-strong);
  border-radius: var(--x-radius);
  background: var(--x-surface-2);
}

.dialog__head {
  padding: 22px 24px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dialog__title {
  font-size: 20px;
  font-weight: 500;
  margin: 0;
}

.dialog__body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 24px 24px;
}

.dialog__field-error {
  font-size: 11.5px;
  color: var(--x-danger);
}

.dialog__meta {
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: var(--x-muted);
}

.dialog__error {
  font-size: 12px;
  color: var(--x-danger);
}

.dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 6px;
}
</style>
