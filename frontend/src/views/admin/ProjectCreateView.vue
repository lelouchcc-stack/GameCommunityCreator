<script setup lang="ts">
/**
 * 新建项目（/admin/projects/new）。
 * 表单：项目名称 / slug / 游戏名称 / 简介；slug 支持从名称自动生成。
 * 视觉：xAI 控制台双栏（左表单 + 右说明/结构预览）。
 */
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { createProject } from '@/api/projects'
import { isApiError } from '@/api/client'
import ToastHost from '@/editor/ToastHost.vue'
import PageHeader from '@/components/admin/PageHeader.vue'
import XIcon from '@/components/admin/XIcon.vue'
import { errorMessageOf } from '@/editor/useEditorState'
import { useToast } from '@/editor/useToast'
import { SLUG_PATTERN } from '@/utils/pageConfig'

const router = useRouter()
const toast = useToast()

const form = reactive({
  name: '',
  slug: '',
  gameName: '',
  description: '',
})

const errors = ref<Record<string, string>>({})
const formError = ref('')
const submitting = ref(false)
const slugTouched = ref(false)

const RANDOM_CHARS = 'abcdefghijklmnopqrstuvwxyz0123456789'

function randomSuffix(length = 4): string {
  let out = ''
  for (let index = 0; index < length; index += 1) {
    out += RANDOM_CHARS[Math.floor(Math.random() * RANDOM_CHARS.length)]
  }
  return out
}

function suggestSlug(name: string): string {
  const base = name
    .toLowerCase()
    .replace(/[\u3400-\u4dbf\u4e00-\u9fff]/g, ' ')
    .replace(/[^a-z0-9\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 50)
    .replace(/-+$/, '')
  return base.length >= 2 ? base : `community-${randomSuffix()}`
}

function onNameInput(event: Event): void {
  form.name = (event.target as HTMLInputElement).value
  if (!slugTouched.value) form.slug = suggestSlug(form.name)
}

function onSlugInput(event: Event): void {
  slugTouched.value = true
  form.slug = (event.target as HTMLInputElement).value
    .toLowerCase()
    .replace(/[^a-z0-9-]/g, '')
    .replace(/^-+/, '')
}

function generateSlug(): void {
  slugTouched.value = true
  form.slug = suggestSlug(form.name || form.gameName)
  delete errors.value.slug
}

function fieldError(key: string): string {
  return errors.value[key] ?? ''
}

function validate(): boolean {
  const next: Record<string, string> = {}
  const name = form.name.trim()
  const slug = form.slug.trim()
  const gameName = form.gameName.trim()

  if (name.length < 1) next.name = '请填写项目名称'
  else if (name.length > 80) next.name = '项目名称不能超过 80 个字符'

  if (slug.length < 1) next.slug = '请填写项目标识'
  else if (!SLUG_PATTERN.test(slug)) {
    next.slug = '项目标识需为 2-63 位小写字母、数字或中划线，且以字母或数字开头'
  }

  if (gameName.length < 1) next.gameName = '请填写游戏名称'
  else if (gameName.length > 80) next.gameName = '游戏名称不能超过 80 个字符'

  if (form.description.length > 500) next.description = '简介不能超过 500 个字符'

  errors.value = next
  return Object.keys(next).length === 0
}

function applyFieldErrors(
  fieldErrors: Record<string, string> | null | undefined,
  message: string,
): void {
  const next: Record<string, string> = {}
  if (fieldErrors) {
    for (const [key, value] of Object.entries(fieldErrors)) {
      if (key === 'name' || key === 'slug' || key === 'gameName' || key === 'description') {
        next[key] = value
      }
    }
  }
  errors.value = next
  if (Object.keys(next).length === 0) formError.value = message
}

async function submit(): Promise<void> {
  if (submitting.value) return
  formError.value = ''
  if (!validate()) return
  submitting.value = true
  try {
    const project = await createProject({
      name: form.name.trim(),
      slug: form.slug.trim(),
      gameName: form.gameName.trim(),
      description: form.description.trim(),
    })
    toast.success(`项目「${project.name}」创建成功：已自动生成 6 个默认页面、默认导航与深色主题`)
    await router.push({ name: 'page-list', params: { id: project.id } })
  } catch (error) {
    if (isApiError(error)) {
      if (error.status === 409) {
        errors.value = {
          ...(error.fieldErrors ?? {}),
          slug: error.fieldErrors?.slug ?? '该项目标识已被占用，请更换一个',
        }
        toast.error('项目标识已被占用，请更换')
      } else if (error.status === 400) {
        applyFieldErrors(error.fieldErrors, error.message)
        toast.error(error.message || '表单校验失败，请检查填写内容')
      } else {
        formError.value = errorMessageOf(error, '创建失败，请稍后重试')
        toast.error(formError.value)
      }
    } else {
      formError.value = errorMessageOf(error, '创建失败，请稍后重试')
      toast.error(formError.value)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="create-page x-page gcc-container">
    <ToastHost />

    <PageHeader kicker="New project" title="创建游戏站点">
      <template #actions>
        <RouterLink class="gcc-btn gcc-btn-sm gcc-btn-ghost" :to="{ name: 'project-list' }">
          返回我的游戏
        </RouterLink>
      </template>
    </PageHeader>

    <div class="create-layout">
      <form class="create-form" @submit.prevent="submit">
        <div class="gcc-field">
          <label class="gcc-label" for="create-name">项目名称</label>
          <input
            id="create-name"
            :value="form.name"
            class="gcc-input"
            maxlength="80"
            placeholder="例如：星汐边境官方社区"
            @input="onNameInput"
          />
          <p v-if="fieldError('name')" class="create-error">{{ fieldError('name') }}</p>
        </div>

        <div class="gcc-field">
          <label class="gcc-label" for="create-slug">项目标识（公开链接）</label>
          <div class="create-slug">
            <span class="create-slug__prefix x-mono">/community/</span>
            <input
              id="create-slug"
              :value="form.slug"
              class="gcc-input create-slug__input x-mono"
              maxlength="63"
              placeholder="stellar-drift"
              @input="onSlugInput"
            />
            <button type="button" class="gcc-btn gcc-btn-sm gcc-btn-ghost" @click="generateSlug">
              <XIcon name="copy" :size="12" />
              重新生成
            </button>
          </div>
          <p v-if="fieldError('slug')" class="create-error">{{ fieldError('slug') }}</p>
          <p v-else class="create-hint x-mono">
            2–63 位小写字母 / 数字 / 中划线，创建后不可修改
          </p>
        </div>

        <div class="gcc-field">
          <label class="gcc-label" for="create-game-name">游戏名称</label>
          <input
            id="create-game-name"
            v-model="form.gameName"
            class="gcc-input"
            maxlength="80"
            placeholder="例如：星汐边境"
          />
          <p v-if="fieldError('gameName')" class="create-error">{{ fieldError('gameName') }}</p>
        </div>

        <div class="gcc-field">
          <label class="gcc-label" for="create-description">项目简介</label>
          <textarea
            id="create-description"
            v-model="form.description"
            class="gcc-textarea"
            maxlength="500"
            placeholder="一句话介绍这款游戏与社区定位（可留空）"
          ></textarea>
          <p v-if="fieldError('description')" class="create-error">{{ fieldError('description') }}</p>
          <p v-else class="create-hint x-mono">{{ form.description.length }} / 500</p>
        </div>

        <p v-if="formError" class="create-form-error">{{ formError }}</p>

        <footer class="create-form__footer">
          <RouterLink class="gcc-btn gcc-btn-ghost" :to="{ name: 'project-list' }">取消</RouterLink>
          <button type="submit" class="gcc-btn gcc-btn-primary" :disabled="submitting">
            {{ submitting ? '创建中…' : '创建站点并进入页面管理' }}
            <XIcon name="arrow-right" :size="14" />
          </button>
        </footer>
      </form>
    </div>
  </div>
</template>

<style scoped>
.create-page {
  padding-top: 36px;
}

.create-layout {
  max-width: 720px;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 26px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
}

.create-slug {
  display: flex;
  align-items: center;
  gap: 8px;
}

.create-slug__prefix {
  flex: none;
  font-size: 11px;
  letter-spacing: 0.02em;
  color: var(--x-muted);
}

.create-slug__input {
  flex: 1;
  min-width: 0;
}

.create-error {
  font-size: 11.5px;
  color: var(--x-danger);
}

.create-hint {
  font-size: 9.5px;
  letter-spacing: 0.06em;
  color: var(--x-muted);
}

.create-form-error {
  font-size: 12px;
  color: var(--x-danger);
}

.create-form__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid var(--x-border);
}
</style>
