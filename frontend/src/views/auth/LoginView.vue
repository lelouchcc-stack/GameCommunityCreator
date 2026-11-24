<script setup lang="ts">
/**
 * 开发者登录页（路由 /login，v4）。
 *
 * - 表单：用户名 / 密码；提交 auth.login（后端校验后签发 JWT，api client 自动持久化并携带）；
 * - 成功：跳转 redirect 查询参数（仅接受站内 /admin 路径）或 /admin/projects；
 * - 失败：401 显示「用户名或密码错误」，400 fieldErrors、网络错误均中文提示；
 * - 支持 ?expired=1（登录状态已过期）与 ?loggedOut=1（已退出登录）提示；
 * - 右上角可切换管理端主题（深色/浅色/跟随系统），与游戏门户主题无关。
 */
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { isApiError } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { ADMIN_THEME_LABELS, useAdminTheme } from '@/composables/useAdminTheme'
import ToastHost from '@/editor/ToastHost.vue'
import { useToast } from '@/editor/useToast'
import type { ThemeMode } from '@/types/site'
import { USERNAME_PATTERN } from '@/types/auth'
import { errorMessageOf } from '@/editor/useEditorState'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const auth = useAuthStore()
const { mode: adminThemeMode, label: adminThemeLabel, setMode: setAdminThemeMode } = useAdminTheme()

const form = ref({ username: '', password: '' })
const showPassword = ref(false)
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref<{ username?: string; password?: string }>({})

/** 登录后回跳地址：只接受站内 /admin 路径，避免开放重定向 */
const redirectTarget = computed(() => {
  const raw = Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect
  if (typeof raw === 'string' && raw.startsWith('/admin') && !raw.startsWith('//')) return raw
  return ''
})

const expiredNotice = computed(() => route.query.expired === '1')
const loggedOutNotice = computed(() => route.query.loggedOut === '1')

const themeModes: ThemeMode[] = ['DARK', 'LIGHT', 'SYSTEM']

function onThemeSelect(event: Event): void {
  setAdminThemeMode((event.target as HTMLSelectElement).value as ThemeMode)
}

function validate(): boolean {
  const errors: { username?: string; password?: string } = {}
  const username = form.value.username.trim()
  if (!username) errors.username = '请填写用户名'
  else if (!USERNAME_PATTERN.test(username)) errors.username = '用户名需为 3-30 位字母、数字、下划线或中划线'
  if (!form.value.password) errors.password = '请填写密码'
  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

async function submit(): Promise<void> {
  if (submitting.value) return
  formError.value = ''
  if (!validate()) return
  submitting.value = true
  try {
    const user = await auth.login({
      username: form.value.username.trim(),
      password: form.value.password,
    })
    toast.success(`欢迎回来，${user.displayName || user.username}`)
    await router.replace(redirectTarget.value || '/admin/projects')
  } catch (error) {
    if (isApiError(error)) {
      if (error.status === 401) {
        formError.value = '用户名或密码错误，请检查后重试'
      } else if (error.status === 400 && Object.keys(error.fieldErrors ?? {}).length > 0) {
        fieldErrors.value = {
          username: error.fieldErrors.username,
          password: error.fieldErrors.password,
        }
        formError.value = error.message || '请检查填写内容'
      } else if (error.status === 403) {
        formError.value = error.message || '无权进行该操作，请确认账号状态'
      } else {
        formError.value = errorMessageOf(error, '登录失败，请稍后重试')
      }
    } else {
      formError.value = errorMessageOf(error, '登录失败，请稍后重试')
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
      <label class="auth-theme" :title="`管理端主题：${adminThemeLabel}`">
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
          <p class="x-kicker">Developer access</p>
          <h1>开发者登录</h1>
        </header>

        <div v-if="expiredNotice" class="auth-notice auth-notice--warn">
          ⚠ 登录状态已过期，请重新登录。
        </div>
        <div v-if="loggedOutNotice" class="auth-notice auth-notice--ok">
          你已安全退出登录，可随时重新登录。
        </div>
        <div v-if="formError" class="gcc-error auth-error">{{ formError }}</div>

        <form class="auth-form" novalidate @submit.prevent="submit">
          <div class="gcc-field">
            <label class="gcc-label" for="login-username">用户名</label>
            <input
              id="login-username"
              v-model="form.username"
              class="gcc-input"
              type="text"
              maxlength="30"
              autocomplete="username"
              placeholder="例如：indie-dev"
            />
            <p v-if="fieldErrors.username" class="auth-field-error">{{ fieldErrors.username }}</p>
          </div>

          <div class="gcc-field">
            <label class="gcc-label" for="login-password">密码</label>
            <div class="auth-password">
              <input
                id="login-password"
                v-model="form.password"
                class="gcc-input"
                :type="showPassword ? 'text' : 'password'"
                maxlength="72"
                autocomplete="current-password"
                placeholder="输入密码"
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
          </div>

          <button type="submit" class="gcc-btn gcc-btn-primary auth-submit" :disabled="submitting">
            {{ submitting ? '登录中…' : '登录' }}
          </button>
        </form>

        <footer class="auth-foot">
          <span>还没有开发者账号？</span>
          <RouterLink
            class="auth-link"
            :to="{
              name: 'auth-register',
              query: redirectTarget ? { redirect: redirectTarget } : undefined,
            }"
          >
            立即注册
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
