<script setup lang="ts">
/**
 * 开发者注册页（路由 /register，v4）。
 *
 * - 表单：用户名 / 显示名 / 密码 / 确认密码；
 * - 前端校验与后端一致：USERNAME_PATTERN、validatePasswordRules（≥8 位且含字母与数字）、两次密码一致；
 * - 提交 auth.register（后端注册成功即签发 JWT 并自动登录）→ /admin/projects；
 * - 409 用户名已存在、400 fieldErrors 均中文提示；
 * - 右上角可切换管理端主题。
 */
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { isApiError } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { ADMIN_THEME_LABELS, useAdminTheme } from '@/composables/useAdminTheme'
import ToastHost from '@/editor/ToastHost.vue'
import { useToast } from '@/editor/useToast'
import type { ThemeMode } from '@/types/site'
import { USERNAME_PATTERN, validatePasswordRules } from '@/types/auth'
import { errorMessageOf } from '@/editor/useEditorState'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const auth = useAuthStore()
const { mode: adminThemeMode, setMode: setAdminThemeMode } = useAdminTheme()

const form = reactive({
  username: '',
  displayName: '',
  password: '',
  confirmPassword: '',
})
const showPassword = ref(false)
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref<Record<string, string>>({})

const loginRedirect = (): Record<string, string> | undefined => {
  const raw = Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect
  return typeof raw === 'string' && raw.startsWith('/admin') && !raw.startsWith('//')
    ? { redirect: raw }
    : undefined
}

const themeModes: ThemeMode[] = ['DARK', 'LIGHT', 'SYSTEM']

function onThemeSelect(event: Event): void {
  setAdminThemeMode((event.target as HTMLSelectElement).value as ThemeMode)
}

function validate(): boolean {
  const errors: Record<string, string> = {}
  const username = form.username.trim()
  const displayName = form.displayName.trim()

  if (!username) errors.username = '请填写用户名'
  else if (!USERNAME_PATTERN.test(username)) {
    errors.username = '用户名需为 3-30 位字母、数字、下划线或中划线'
  }

  if (!displayName) errors.displayName = '请填写显示名'
  else if (displayName.length > 60) errors.displayName = '显示名不能超过 60 个字符'

  const passwordErrors = validatePasswordRules(form.password)
  if (passwordErrors.length > 0) errors.password = passwordErrors.join('；')
  if (!form.confirmPassword) errors.confirmPassword = '请再次输入密码'
  else if (form.password !== form.confirmPassword) errors.confirmPassword = '两次输入的密码不一致'

  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

async function submit(): Promise<void> {
  if (submitting.value) return
  formError.value = ''
  fieldErrors.value = {}
  if (!validate()) return
  submitting.value = true
  try {
    const user = await auth.register({
      username: form.username.trim(),
      password: form.password,
      displayName: form.displayName.trim(),
    })
    toast.success(`注册成功，欢迎加入，${user.displayName || user.username}`)
    await router.replace(loginRedirect() ?? '/admin/projects')
  } catch (error) {
    if (isApiError(error)) {
      if (error.status === 409) {
        fieldErrors.value = {
          ...(error.fieldErrors ?? {}),
          username: error.fieldErrors?.username ?? '该用户名已被注册，请更换一个',
        }
        formError.value = '用户名已存在，请更换后重试'
      } else if (error.status === 400) {
        fieldErrors.value = { ...(error.fieldErrors ?? {}) }
        formError.value = error.message || '注册信息校验失败，请检查后重试'
      } else {
        formError.value = errorMessageOf(error, '注册失败，请稍后重试')
      }
    } else {
      formError.value = errorMessageOf(error, '注册失败，请稍后重试')
    }
    toast.error(formError.value)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <ToastHost />

    <div class="auth-topbar">
      <RouterLink to="/community" class="auth-brand">
        <span class="auth-brand__mark" aria-hidden="true">G</span>
        <span class="auth-brand__text">
          <span class="auth-brand__name">Game Community Creator</span>
        </span>
      </RouterLink>
      <label class="auth-theme" :title="`管理端主题：${ADMIN_THEME_LABELS[adminThemeMode]}`">
        <span aria-hidden="true">{{
          adminThemeMode === 'LIGHT' ? '☀' : adminThemeMode === 'DARK' ? '🌙' : '🖥'
        }}</span>
        <select
          class="auth-theme__select"
          :value="adminThemeMode"
          aria-label="管理端主题"
          @change="onThemeSelect"
        >
          <option v-for="item in themeModes" :key="item" :value="item">
            {{ ADMIN_THEME_LABELS[item] }}
          </option>
        </select>
      </label>
    </div>

    <main class="auth-main">
      <section class="auth-card">
        <header class="auth-head">
          <p class="x-kicker">Create developer account</p>
          <h1>创建开发者账号</h1>
        </header>

        <div v-if="formError" class="gcc-error auth-error">{{ formError }}</div>

        <form class="auth-form" novalidate @submit.prevent="submit">
          <div class="gcc-field">
            <label class="gcc-label" for="register-username">用户名</label>
            <input
              id="register-username"
              v-model="form.username"
              class="gcc-input"
              type="text"
              maxlength="30"
              autocomplete="username"
              placeholder="3-30 位字母、数字、下划线或中划线"
            />
            <p v-if="fieldErrors.username" class="auth-field-error">{{ fieldErrors.username }}</p>
          </div>

          <div class="gcc-field">
            <label class="gcc-label" for="register-display-name">显示名</label>
            <input
              id="register-display-name"
              v-model="form.displayName"
              class="gcc-input"
              type="text"
              maxlength="60"
              autocomplete="nickname"
              placeholder="展示在管理端顶部的名字，例如：独立开发者"
            />
            <div class="auth-field-meta">
              <span v-if="fieldErrors.displayName" class="auth-field-error">
                {{ fieldErrors.displayName }}
              </span>
              <span>{{ form.displayName.length }}/60</span>
            </div>
          </div>

          <div class="gcc-field">
            <label class="gcc-label" for="register-password">密码</label>
            <div class="auth-password">
              <input
                id="register-password"
                v-model="form.password"
                class="gcc-input"
                :type="showPassword ? 'text' : 'password'"
                maxlength="72"
                autocomplete="new-password"
                placeholder="至少 8 位，需包含字母与数字"
              />
              <button
                type="button"
                class="auth-password__toggle"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? '隐藏' : '显示' }}
              </button>
            </div>
            <p v-if="fieldErrors.password" class="auth-field-error">{{ fieldErrors.password }}</p>
            <p v-else class="auth-field-help">至少 8 位，且同时包含字母与数字。</p>
          </div>

          <div class="gcc-field">
            <label class="gcc-label" for="register-confirm">确认密码</label>
            <input
              id="register-confirm"
              v-model="form.confirmPassword"
              class="gcc-input"
              :type="showPassword ? 'text' : 'password'"
              maxlength="72"
              autocomplete="new-password"
              placeholder="再次输入密码"
            />
            <p v-if="fieldErrors.confirmPassword" class="auth-field-error">
              {{ fieldErrors.confirmPassword }}
            </p>
          </div>

          <button type="submit" class="gcc-btn gcc-btn-primary auth-submit" :disabled="submitting">
            {{ submitting ? '注册中…' : '注册并登录' }}
          </button>
        </form>

        <footer class="auth-foot">
          <span>已经有开发者账号？</span>
          <RouterLink class="auth-link" :to="{ name: 'auth-login', query: loginRedirect() }">
            返回登录
          </RouterLink>
        </footer>

      </section>
    </main>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--x-canvas);
  color: var(--x-text);
}

/* ---------------- topbar ---------------- */
.auth-topbar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 28px;
}

.auth-brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  color: var(--x-text);
}

.auth-brand:hover {
  color: var(--x-text);
}

.auth-brand__mark {
  flex: none;
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--x-border-strong);
  border-radius: 6px;
  background: transparent;
  color: var(--x-text);
  font-family: var(--x-mono);
  font-size: 14px;
  font-weight: 600;
}

.auth-brand__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 1px;
}

.auth-brand__name {
  font-family: var(--x-mono);
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  line-height: 1.4;
}

.auth-theme {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 12px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-pill);
  background: transparent;
  color: var(--x-text-secondary);
  font-size: 11px;
}

.auth-theme__select {
  border: none;
  background: transparent;
  color: var(--x-text-secondary);
  font-size: 11px;
  outline: none;
  cursor: pointer;
}

/* ---------------- main ---------------- */
.auth-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px 48px;
}

.auth-card {
  width: min(420px, 100%);
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 32px 32px 26px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
}

.auth-head {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.auth-head h1 {
  margin: 0;
  font-size: clamp(28px, 3vw, 36px);
  font-weight: 400;
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: var(--x-text);
}

/* ---------------- notices ---------------- */
.auth-notice {
  padding: 9px 12px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: transparent;
  font-size: 12px;
  line-height: 1.6;
  color: var(--x-text-secondary);
  overflow-wrap: anywhere;
}

.auth-notice--warn {
  border-color: rgba(255, 176, 32, 0.4);
  background: rgba(255, 176, 32, 0.06);
  color: var(--page-warning-fg, var(--x-warning));
}

.auth-notice--ok {
  border-color: rgba(61, 220, 151, 0.35);
  background: rgba(61, 220, 151, 0.06);
  color: var(--page-success-fg, var(--x-success));
}

.auth-error {
  align-items: flex-start;
  text-align: left;
  padding: 10px 12px;
  border-radius: var(--x-radius);
}

/* ---------------- form ---------------- */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.auth-password {
  position: relative;
  display: flex;
  align-items: center;
}

.auth-password .gcc-input {
  padding-right: 64px;
}

.auth-password__toggle {
  position: absolute;
  right: 7px;
  padding: 4px 9px;
  border: 1px solid transparent;
  border-radius: var(--x-radius-pill);
  background: transparent;
  color: var(--x-muted);
  font-size: 10.5px;
  letter-spacing: 0.04em;
  cursor: pointer;
}

.auth-password__toggle:hover {
  color: var(--x-text);
  border-color: var(--x-border);
}

.auth-field-error {
  font-size: 10.5px;
  color: var(--page-danger-fg, var(--x-danger));
}

.auth-field-help {
  font-size: 10.5px;
  color: var(--x-muted);
}

.auth-field-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.04em;
  color: var(--x-muted);
}

.auth-submit {
  width: 100%;
  margin-top: 4px;
  padding: 11px 18px;
  font-size: 13.5px;
}

/* ---------------- foot ---------------- */
.auth-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
  color: var(--x-muted);
}

.auth-link {
  color: var(--x-text);
  font-weight: 500;
  text-decoration: underline;
  text-decoration-color: var(--x-border-strong);
  text-underline-offset: 4px;
}

.auth-link:hover {
  color: var(--x-accent);
  text-decoration-color: var(--x-accent);
}

:global(.admin-theme-scope[data-admin-theme='light'] .auth-page) {
  --page-danger-fg: #b42318;
  --page-warning-fg: #8a5a00;
  --page-success-fg: #0f7a4e;
}

@media (max-width: 560px) {
  .auth-topbar {
    padding: 14px 16px;
  }

  .auth-card {
    padding: 26px 20px 22px;
  }
}
</style>
