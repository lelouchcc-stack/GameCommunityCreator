<script setup lang="ts">
/**
 * 站点设置（路由 /admin/projects/:id/settings）：深浅色主题 + 顶部/底部导航管理。
 *
 * - 主题模式：DARK / LIGHT / SYSTEM，保存 saveSiteSettings；站点默认模式 + 访客可在公开页切换；
 * - 导航：header / footer 两组，条目 标题/目标类型(page|external)/目标页面/URL/显隐/上下移/删除/新增，
 *   整体保存 saveNavigation（sortOrder 按数组顺序重算）；目标页面已删除/停用时显示明确错误；
 * - 所有错误以中文 message / fieldErrors 展示。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { isApiError } from '@/api/client'
import { getProject } from '@/api/projects'
import { listPages } from '@/api/pages'
import { getNavigation, getSiteSettings, saveNavigation, saveSiteSettings } from '@/api/site'
import ToastHost from '@/editor/ToastHost.vue'
import PageHeader from '@/components/admin/PageHeader.vue'
import XIcon from '@/components/admin/XIcon.vue'
import { errorMessageOf } from '@/editor/useEditorState'
import { useToast } from '@/editor/useToast'
import type { ProjectView } from '@/types/api'
import {
  PAGE_STATUS_LABELS,
  PAGE_TYPE_LABELS,
  THEME_MODES,
  THEME_MODE_LABELS,
  type NavigationItemInput,
  type NavigationLocation,
  type NavigationTargetType,
  type SitePageView,
  type SiteSettings,
  type ThemeMode,
} from '@/types/site'
import { isValidHttpUrl } from '@/utils/actions'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const rawId = Number(route.params.id)
const invalidId = !Number.isInteger(rawId) || rawId <= 0
const projectId = invalidId ? 0 : rawId

/* ------------------------------ 导航行模型 ------------------------------ */

interface NavRow {
  uid: number
  location: NavigationLocation
  title: string
  targetType: NavigationTargetType
  targetPageId: string
  url: string
  visible: boolean
}

let uidSeed = 0
function nextUid(): number {
  uidSeed += 1
  return uidSeed
}

const project = ref<ProjectView | null>(null)
const pages = ref<SitePageView[]>([])
const settings = ref<SiteSettings | null>(null)
const rows = ref<NavRow[]>([])
/** 最近一次加载/保存后的导航序列化快照，用于判断「有未保存修改」 */
const navBaseline = ref('')

const loading = ref(true)
const loadError = ref('')
const loadErrorStatus = ref(0)

const themeMode = ref<ThemeMode>('DARK')
const savingTheme = ref(false)
const savingNav = ref(false)
/** 后端 fieldErrors：uid → 字段 → 中文原因 */
const navFieldErrors = ref<Record<number, Record<string, string>>>({})
const navSaveError = ref('')

const pageById = computed(() => new Map(pages.value.map((item) => [item.pageId, item])))

function displayName(page: SitePageView): string {
  const type = PAGE_TYPE_LABELS[page.pageType] ?? page.pageType
  const status = PAGE_STATUS_LABELS[page.status] ?? page.status
  const draft = page.status === 'DRAFT' ? ' · 未发布' : ''
  return `${page.name}（${type} · ${status}${draft}）`
}

const headerRows = computed(() => rows.value.filter((row) => row.location === 'header'))
const footerRows = computed(() => rows.value.filter((row) => row.location === 'footer'))

const themeDirty = computed(() => settings.value !== null && themeMode.value !== settings.value.themeMode)

/** 按数组顺序重算 sortOrder，得到可直接 PUT 的 payload */
function buildNavItems(): NavigationItemInput[] {
  const items: NavigationItemInput[] = []
  for (const location of ['header', 'footer'] as NavigationLocation[]) {
    rows.value
      .filter((row) => row.location === location)
      .forEach((row, index) => {
        items.push({
          location,
          title: row.title.trim(),
          targetType: row.targetType,
          targetPageId: row.targetType === 'page' ? row.targetPageId || null : null,
          url: row.targetType === 'external' ? row.url.trim() : '',
          sortOrder: index,
          visible: row.visible,
        })
      })
  }
  return items
}

const navDirty = computed(() => JSON.stringify(buildNavItems()) !== navBaseline.value)

/* ------------------------------ 加载 ------------------------------ */

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  loadErrorStatus.value = 0
  try {
    const [projectData, pageList, navigationData, siteSettings] = await Promise.all([
      getProject(projectId),
      listPages(projectId),
      getNavigation(projectId),
      getSiteSettings(projectId),
    ])
    project.value = projectData
    pages.value = pageList
    settings.value = siteSettings
    themeMode.value = siteSettings.themeMode
    rows.value = navigationData.items.map((item) => ({
      uid: nextUid(),
      location: item.location,
      title: item.title,
      targetType: item.targetType,
      targetPageId: item.targetPageId ?? '',
      url: item.url ?? '',
      visible: item.visible,
    }))
    navBaseline.value = JSON.stringify(buildNavItems())
    navFieldErrors.value = {}
    navSaveError.value = ''
  } catch (error) {
    loadError.value = errorMessageOf(error, '加载站点设置失败，请稍后重试')
    loadErrorStatus.value = isApiError(error) ? error.status : 0
  } finally {
    loading.value = false
  }
}

/* ------------------------------ 导航编辑 ------------------------------ */

function targetPageOf(row: NavRow): SitePageView | undefined {
  return pageById.value.get(row.targetPageId)
}

/**
 * 导航行的即时问题（空 / 目标已删除 / 目标已停用 / URL 非法），
 * 既用于行内红字提示，也用于保存前阻断。
 */
function rowProblems(row: NavRow): string[] {
  const problems: string[] = []
  const title = row.title.trim()
  if (!title) problems.push('标题不能为空')
  else if (title.length > 40) problems.push('标题不能超过 40 个字符')

  if (row.targetType === 'page') {
    if (!row.targetPageId) {
      problems.push('请选择目标页面')
    } else {
      const target = targetPageOf(row)
      if (!target) problems.push('目标页面已被删除，请重新选择')
      else if (target.status === 'DISABLED') problems.push('目标页面已停用，请重新选择')
      else if (target.status === 'DRAFT') problems.push('目标页面尚未发布：公开导航中会自动隐藏（保存仍允许）')
    }
  } else {
    if (!row.url.trim()) problems.push('请填写外部链接 URL')
    else if (!isValidHttpUrl(row.url)) problems.push('外部链接必须是合法的 http/https 绝对地址')
  }
  return problems
}

/** 阻断保存的问题（「尚未发布」只是提示，不阻断） */
function rowBlocksSave(row: NavRow): string[] {
  return rowProblems(row).filter((item) => !item.includes('尚未发布'))
}

function onNavTitleInput(row: NavRow, event: Event): void {
  row.title = (event.target as HTMLInputElement).value
}

function onNavTypeChange(row: NavRow, event: Event): void {
  row.targetType = (event.target as HTMLSelectElement).value as NavigationTargetType
  // 切换目标类型时清空另一类字段，避免把 page 的 id 带到 external 里
  if (row.targetType === 'page') {
    row.url = ''
  } else {
    row.targetPageId = ''
  }
}

function onNavTargetChange(row: NavRow, event: Event): void {
  row.targetPageId = (event.target as HTMLSelectElement).value
}

function onNavUrlInput(row: NavRow, event: Event): void {
  row.url = (event.target as HTMLInputElement).value
}

function onNavVisibleChange(row: NavRow, event: Event): void {
  row.visible = (event.target as HTMLInputElement).checked
}

function addNavRow(location: NavigationLocation): void {
  const count = rows.value.filter((row) => row.location === location).length
  if (count >= 20) {
    toast.error('每个位置最多 20 条导航')
    return
  }
  rows.value.push({
    uid: nextUid(),
    location,
    title: location === 'header' ? '新导航' : '新页脚链接',
    targetType: 'page',
    targetPageId: '',
    url: '',
    visible: true,
  })
}

function removeNavRow(row: NavRow): void {
  rows.value = rows.value.filter((item) => item.uid !== row.uid)
}

/** 在所属分组内上移/下移一位 */
function moveNavRow(row: NavRow, delta: number): void {
  const group = rows.value.filter((item) => item.location === row.location)
  const groupIndex = group.findIndex((item) => item.uid === row.uid)
  const targetGroupIndex = groupIndex + delta
  if (groupIndex < 0 || targetGroupIndex < 0 || targetGroupIndex >= group.length) return
  const target = group[targetGroupIndex]
  if (!target) return
  const from = rows.value.findIndex((item) => item.uid === row.uid)
  const to = rows.value.findIndex((item) => item.uid === target.uid)
  if (from < 0 || to < 0) return
  const [moved] = rows.value.splice(from, 1)
  if (!moved) return
  rows.value.splice(to, 0, moved)
}

/* ------------------------------ 保存 ------------------------------ */

async function onSaveNav(): Promise<void> {
  if (savingNav.value) return
  navSaveError.value = ''
  navFieldErrors.value = {}

  if (rows.value.length === 0) {
    toast.error('导航不能为空：至少保留一条站内页面或外部链接')
    return
  }
  const blockers = rows.value.flatMap((row) => rowBlocksSave(row))
  if (blockers.length > 0) {
    navSaveError.value = `导航有 ${blockers.length} 处问题，请先修正：${blockers[0]}`
    toast.error('导航校验未通过，请按行内提示修正后再保存')
    return
  }

  savingNav.value = true
  try {
    const result = await saveNavigation(projectId, buildNavItems())
    rows.value = result.items.map((item) => ({
      uid: nextUid(),
      location: item.location,
      title: item.title,
      targetType: item.targetType,
      targetPageId: item.targetPageId ?? '',
      url: item.url ?? '',
      visible: item.visible,
    }))
    navBaseline.value = JSON.stringify(buildNavItems())
    navFieldErrors.value = {}
    toast.success(`导航已保存：顶部 ${headerRows.value.length} 条 / 底部 ${footerRows.value.length} 条`)
  } catch (error) {
    if (isApiError(error)) {
      const fieldErrors = error.fieldErrors ?? {}
      const mapped: Record<number, Record<string, string>> = {}
      // 保存时 items 的顺序 = 顶部行在前、底部行在后，按同样顺序反查行
      const orderedRows = [...headerRows.value, ...footerRows.value]
      for (const [key, message] of Object.entries(fieldErrors)) {
        const match = /^items\[(\d+)\]\.(.+)$/.exec(key)
        if (!match) continue
        const index = Number(match[1])
        const field = match[2] ?? 'targetPageId'
        const row = orderedRows[index]
        if (!row) continue
        mapped[row.uid] = mapped[row.uid] ?? {}
        mapped[row.uid][field] = message
      }
      navFieldErrors.value = mapped
      navSaveError.value = error.message || '导航保存失败，请检查后重试'
      toast.error(navSaveError.value)
    } else {
      navSaveError.value = errorMessageOf(error, '导航保存失败，请稍后重试')
      toast.error(navSaveError.value)
    }
  } finally {
    savingNav.value = false
  }
}

async function onSaveTheme(): Promise<void> {
  if (savingTheme.value) return
  savingTheme.value = true
  try {
    const updated = await saveSiteSettings(projectId, { themeMode: themeMode.value })
    settings.value = updated
    themeMode.value = updated.themeMode
    toast.success(`主题已保存：${THEME_MODE_LABELS[updated.themeMode]}`)
  } catch (error) {
    toast.error(`主题保存失败：${errorMessageOf(error, '请稍后重试')}`)
  } finally {
    savingTheme.value = false
  }
}

/* ------------------------------ 导航 ------------------------------ */

function goBack(): void {
  void router.push({ name: 'project-list' })
}

function goPages(): void {
  void router.push({ name: 'page-list', params: { id: projectId } })
}

onMounted(() => {
  if (invalidId) {
    loading.value = false
    loadError.value = '地址中的站点 ID 不合法，请从站点列表进入'
    return
  }
  void load()
})
</script>

<template>
  <div class="settings-page x-page gcc-container">
    <ToastHost />

    <PageHeader
      kicker="Site settings"
      :title="project ? `站点设置 · ${project.name}` : '站点设置'"
    >
      <template #actions>
        <button type="button" class="gcc-btn gcc-btn-sm gcc-btn-ghost" @click="goBack">
          ← 站点列表
        </button>
        <button type="button" class="gcc-btn gcc-btn-sm" @click="goPages">页面管理</button>
      </template>
    </PageHeader>

    <div v-if="project" class="settings-meta">
      <span class="x-meta">GAME / {{ project.gameName }} · SLUG /{{ project.slug }}</span>
      <span class="gcc-spacer" />
      <span class="x-meta">DEFAULT THEME / {{ settings ? THEME_MODE_LABELS[settings.themeMode] : '—' }}</span>
    </div>

    <!-- 加载中 -->
    <section v-if="loading" class="state-block" aria-busy="true">
      <div class="gcc-skeleton skeleton-block" />
      <div class="gcc-skeleton skeleton-block" />
    </section>

    <!-- 加载失败 -->
    <section v-else-if="loadError" class="gcc-error state-block">
      <p>{{ loadError }}</p>
      <div class="state-actions">
        <button type="button" class="gcc-btn" @click="load">重试</button>
        <button type="button" class="gcc-btn gcc-btn-ghost" @click="goBack">返回站点列表</button>
      </div>
    </section>

    <template v-else>
      <!-- ============================ 主题模式 ============================ -->
      <section class="settings-panel">
        <header class="panel-head">
          <div class="panel-head__text">
            <p class="x-kicker">Appearance</p>
            <h2 class="panel-title">深浅色主题</h2>
          </div>
          <button
            type="button"
            class="gcc-btn gcc-btn-sm gcc-btn-primary"
            :disabled="savingTheme || !themeDirty"
            :title="themeDirty ? '保存主题模式' : '主题模式未变化'"
            @click="onSaveTheme"
          >
            {{ savingTheme ? '保存中…' : '保存主题' }}
          </button>
        </header>

        <div class="theme-options" role="radiogroup" aria-label="主题模式">
          <label
            v-for="mode in THEME_MODES"
            :key="mode"
            class="theme-option"
            :class="{ 'is-active': themeMode === mode }"
          >
            <input v-model="themeMode" type="radio" name="theme-mode" :value="mode" />
            <span class="theme-option-body">
              <span class="theme-option-title">{{ THEME_MODE_LABELS[mode] }}</span>
            </span>
          </label>
        </div>
        <p class="panel-hint x-mono">
          当前已保存：{{ settings ? THEME_MODE_LABELS[settings.themeMode] : '—' }}
          <template v-if="themeDirty">（有未保存的修改）</template>
        </p>
      </section>

      <!-- ============================ 导航管理 ============================ -->
      <section class="settings-panel">
        <header class="panel-head">
          <div class="panel-head__text">
            <p class="x-kicker">Navigation</p>
            <h2 class="panel-title">导航管理</h2>
          </div>
          <button
            type="button"
            class="gcc-btn gcc-btn-sm gcc-btn-primary"
            :disabled="savingNav || !navDirty"
            :title="navDirty ? '保存全部导航（整体替换）' : '导航未变化'"
            @click="onSaveNav"
          >
            {{ savingNav ? '保存中…' : '保存导航' }}
          </button>
        </header>

        <p v-if="navSaveError" class="gcc-error nav-error">{{ navSaveError }}</p>

        <!-- 顶部导航 -->
        <div class="nav-group">
          <div class="nav-group-head">
            <h3 class="nav-group-title">顶部导航</h3>
            <span class="x-kicker">HEADER</span>
            <span class="gcc-badge">{{ headerRows.length }}/20</span>
            <span class="gcc-spacer" />
            <button
              type="button"
              class="gcc-btn gcc-btn-sm"
              :disabled="headerRows.length >= 20"
              @click="addNavRow('header')"
            >
              <XIcon name="plus" :size="13" />
              新增顶部条目
            </button>
          </div>

          <div v-if="headerRows.length === 0" class="gcc-empty nav-empty">顶部导航暂无条目</div>

          <div v-for="row in headerRows" :key="row.uid" class="nav-row">
            <div class="nav-fields">
              <div class="gcc-field nav-title-field">
                <label class="gcc-label">标题</label>
                <input
                  class="gcc-input"
                  type="text"
                  maxlength="40"
                  :value="row.title"
                  placeholder="例如：首页"
                  @input="onNavTitleInput(row, $event)"
                />
              </div>

              <div class="gcc-field nav-type-field">
                <label class="gcc-label">目标类型</label>
                <select class="gcc-select" :value="row.targetType" @change="onNavTypeChange(row, $event)">
                  <option value="page">站内页面</option>
                  <option value="external">外部链接</option>
                </select>
              </div>

              <div v-if="row.targetType === 'page'" class="gcc-field nav-target-field">
                <label class="gcc-label">目标页面</label>
                <select
                  class="gcc-select"
                  :value="row.targetPageId"
                  @change="onNavTargetChange(row, $event)"
                >
                  <option value="" disabled>请选择目标页面</option>
                  <option
                    v-for="page in pages"
                    :key="page.pageId"
                    :value="page.pageId"
                    :disabled="page.status === 'DISABLED'"
                    :title="page.status === 'DISABLED' ? '该页面已停用，不能作为导航目标' : undefined"
                  >
                    {{ displayName(page) }}
                  </option>
                  <option v-if="row.targetPageId && !targetPageOf(row)" :value="row.targetPageId">
                    ⚠ 目标页面已删除（{{ row.targetPageId }}）
                  </option>
                </select>
              </div>

              <div v-else class="gcc-field nav-url-field">
                <label class="gcc-label">外部链接 URL</label>
                <input
                  class="gcc-input x-mono"
                  type="text"
                  maxlength="300"
                  :value="row.url"
                  placeholder="https://example.com"
                  @input="onNavUrlInput(row, $event)"
                />
              </div>

              <label class="gcc-checkbox nav-visible-field">
                <input
                  type="checkbox"
                  :checked="row.visible"
                  @change="onNavVisibleChange(row, $event)"
                />
                <span>显示</span>
              </label>

              <div class="nav-row-actions">
                <button
                  type="button"
                  class="icon-btn"
                  title="上移"
                  aria-label="上移"
                  :disabled="headerRows[0]?.uid === row.uid"
                  @click="moveNavRow(row, -1)"
                >
                  ↑
                </button>
                <button
                  type="button"
                  class="icon-btn"
                  title="下移"
                  aria-label="下移"
                  :disabled="headerRows[headerRows.length - 1]?.uid === row.uid"
                  @click="moveNavRow(row, 1)"
                >
                  ↓
                </button>
                <button
                  type="button"
                  class="icon-btn danger"
                  title="删除该条目"
                  aria-label="删除条目"
                  @click="removeNavRow(row)"
                >
                  ✕
                </button>
              </div>
            </div>

            <div v-if="rowProblems(row).length > 0" class="nav-row-errors">
              <span
                v-for="problem in rowProblems(row)"
                :key="problem"
                class="nav-row-error"
                :class="{ 'is-warn': problem.includes('尚未发布') }"
              >
                {{ problem }}
              </span>
            </div>
            <div v-if="Object.keys(navFieldErrors[row.uid] ?? {}).length > 0" class="nav-row-errors">
              <span
                v-for="(message, key) in navFieldErrors[row.uid]"
                :key="key"
                class="nav-row-error"
              >
                {{ message }}
              </span>
            </div>
          </div>
        </div>

        <!-- 底部导航 -->
        <div class="nav-group">
          <div class="nav-group-head">
            <h3 class="nav-group-title">底部导航</h3>
            <span class="x-kicker">FOOTER</span>
            <span class="gcc-badge">{{ footerRows.length }}/20</span>
            <span class="gcc-spacer" />
            <button
              type="button"
              class="gcc-btn gcc-btn-sm"
              :disabled="footerRows.length >= 20"
              @click="addNavRow('footer')"
            >
              <XIcon name="plus" :size="13" />
              新增底部条目
            </button>
          </div>

          <div v-if="footerRows.length === 0" class="gcc-empty nav-empty">底部导航暂无条目</div>

          <div v-for="row in footerRows" :key="row.uid" class="nav-row">
            <div class="nav-fields">
              <div class="gcc-field nav-title-field">
                <label class="gcc-label">标题</label>
                <input
                  class="gcc-input"
                  type="text"
                  maxlength="40"
                  :value="row.title"
                  placeholder="例如：问题反馈"
                  @input="onNavTitleInput(row, $event)"
                />
              </div>

              <div class="gcc-field nav-type-field">
                <label class="gcc-label">目标类型</label>
                <select class="gcc-select" :value="row.targetType" @change="onNavTypeChange(row, $event)">
                  <option value="page">站内页面</option>
                  <option value="external">外部链接</option>
                </select>
              </div>

              <div v-if="row.targetType === 'page'" class="gcc-field nav-target-field">
                <label class="gcc-label">目标页面</label>
                <select
                  class="gcc-select"
                  :value="row.targetPageId"
                  @change="onNavTargetChange(row, $event)"
                >
                  <option value="" disabled>请选择目标页面</option>
                  <option
                    v-for="page in pages"
                    :key="page.pageId"
                    :value="page.pageId"
                    :disabled="page.status === 'DISABLED'"
                  >
                    {{ displayName(page) }}
                  </option>
                  <option v-if="row.targetPageId && !targetPageOf(row)" :value="row.targetPageId">
                    ⚠ 目标页面已删除（{{ row.targetPageId }}）
                  </option>
                </select>
              </div>

              <div v-else class="gcc-field nav-url-field">
                <label class="gcc-label">外部链接 URL</label>
                <input
                  class="gcc-input x-mono"
                  type="text"
                  maxlength="300"
                  :value="row.url"
                  placeholder="https://example.com"
                  @input="onNavUrlInput(row, $event)"
                />
              </div>

              <label class="gcc-checkbox nav-visible-field">
                <input
                  type="checkbox"
                  :checked="row.visible"
                  @change="onNavVisibleChange(row, $event)"
                />
                <span>显示</span>
              </label>

              <div class="nav-row-actions">
                <button
                  type="button"
                  class="icon-btn"
                  title="上移"
                  aria-label="上移"
                  :disabled="footerRows[0]?.uid === row.uid"
                  @click="moveNavRow(row, -1)"
                >
                  ↑
                </button>
                <button
                  type="button"
                  class="icon-btn"
                  title="下移"
                  aria-label="下移"
                  :disabled="footerRows[footerRows.length - 1]?.uid === row.uid"
                  @click="moveNavRow(row, 1)"
                >
                  ↓
                </button>
                <button
                  type="button"
                  class="icon-btn danger"
                  title="删除该条目"
                  aria-label="删除条目"
                  @click="removeNavRow(row)"
                >
                  ✕
                </button>
              </div>
            </div>

            <div v-if="rowProblems(row).length > 0" class="nav-row-errors">
              <span
                v-for="problem in rowProblems(row)"
                :key="problem"
                class="nav-row-error"
                :class="{ 'is-warn': problem.includes('尚未发布') }"
              >
                {{ problem }}
              </span>
            </div>
            <div v-if="Object.keys(navFieldErrors[row.uid] ?? {}).length > 0" class="nav-row-errors">
              <span
                v-for="(message, key) in navFieldErrors[row.uid]"
                :key="key"
                class="nav-row-error"
              >
                {{ message }}
              </span>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.settings-page {
  --page-danger-fg: var(--x-danger);
  --page-warning-fg: var(--x-warning);
  padding-top: 36px;
}

.settings-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 22px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--x-border);
}

.state-block {
  margin-top: 8px;
}

.state-actions {
  display: flex;
  gap: 10px;
}

.skeleton-block {
  height: 140px;
  margin-bottom: 14px;
}

/* ---------------- panels ---------------- */
.settings-panel {
  margin-bottom: 20px;
  padding: 22px 24px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--x-border);
}

.panel-head__text {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.panel-title {
  margin: 0;
  font-size: 20px;
  font-weight: 400;
  letter-spacing: -0.015em;
}

.panel-desc {
  max-width: 720px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--x-muted);
}

.panel-hint {
  margin-top: 14px;
  font-size: 10px;
  letter-spacing: 0.05em;
  color: var(--x-muted);
}

/* ---------------- theme ---------------- */
.theme-options {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.theme-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-canvas-soft);
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.theme-option:hover {
  border-color: var(--x-border-strong);
}

.theme-option.is-active {
  border-color: var(--x-accent);
  background: var(--x-accent-soft);
}

.theme-option input {
  margin-top: 3px;
  width: 14px;
  height: 14px;
  accent-color: var(--x-accent);
}

.theme-option-body {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.theme-option-title {
  font-size: 13.5px;
  font-weight: 500;
}

.theme-option-desc {
  font-size: 11px;
  line-height: 1.6;
  color: var(--x-muted);
}

/* ---------------- nav groups ---------------- */
.nav-error {
  margin-bottom: 16px;
}

.nav-group {
  padding-top: 18px;
  border-top: 1px solid var(--x-border);
}

.nav-group:first-of-type {
  padding-top: 0;
  border-top: none;
}

.nav-group-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.nav-group-title {
  font-size: 14px;
  font-weight: 500;
}

.nav-empty {
  margin-bottom: 10px;
  padding: 18px;
}

.nav-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 0;
  border-top: 1px solid var(--x-border);
}

.nav-row:first-of-type {
  border-top: none;
}

.nav-fields {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.nav-title-field {
  width: 180px;
}

.nav-type-field {
  width: 132px;
}

.nav-target-field,
.nav-url-field {
  flex: 1;
  min-width: 240px;
}

.nav-visible-field {
  padding-bottom: 10px;
}

.nav-row-actions {
  display: flex;
  gap: 4px;
  padding-bottom: 5px;
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border-radius: 50%;
  border: 1px solid var(--x-border);
  background: transparent;
  color: var(--x-muted);
  font-size: 12px;
  cursor: pointer;
  transition:
    color 0.15s ease,
    border-color 0.15s ease,
    background 0.15s ease;
}

.icon-btn:hover:not(:disabled) {
  color: var(--x-text);
  border-color: var(--x-border-strong);
  background: var(--x-surface-2);
}

.icon-btn.danger:hover:not(:disabled) {
  color: var(--x-danger);
  border-color: rgba(242, 95, 92, 0.5);
}

.icon-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.nav-row-errors {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.nav-row-error {
  font-size: 11px;
  line-height: 1.5;
  color: var(--page-danger-fg);
}

.nav-row-error.is-warn {
  color: var(--page-warning-fg);
}

:global(.admin-theme-scope[data-admin-theme='light'] .settings-page) {
  --page-danger-fg: #b42318;
  --page-warning-fg: #8a5a00;
}

@media (max-width: 1024px) {
  .theme-options {
    grid-template-columns: 1fr;
  }

  .nav-title-field,
  .nav-type-field {
    width: 100%;
  }
}
</style>
