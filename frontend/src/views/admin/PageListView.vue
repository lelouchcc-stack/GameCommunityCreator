<script setup lang="ts">
/**
 * 页面管理（路由 /admin/projects/:id/pages）。
 *
 * - 顶部：返回站点列表、站点名/游戏名、站点设置入口、站点发布按钮；
 * - 页面列表：名称/slug/类型/状态徽标/首页标记/草稿 vN vs 发布 vN/更新时间；
 * - 操作：打开编辑器、草稿预览、发布页面、复制、编辑元信息、删除（home 不可删）、状态切换；
 * - 新建页面对话框：pageType（home 不可选、模块类型已存在则禁用）、名称、custom slug、模板（只读）；
 * - 所有错误（400/409/网络）以中文 message / fieldErrors 展示。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { isApiError } from '@/api/client'
import { getProject } from '@/api/projects'
import {
  copyPage,
  createPage,
  deletePage,
  listPages,
  publishPage,
  publishSite,
  updatePage,
} from '@/api/pages'
import ToastHost from '@/editor/ToastHost.vue'
import PageHeader from '@/components/admin/PageHeader.vue'
import XIcon from '@/components/admin/XIcon.vue'
import { errorMessageOf } from '@/editor/useEditorState'
import { useToast } from '@/editor/useToast'
import type { ProjectView } from '@/types/api'
import {
  DEFAULT_TEMPLATE_BY_PAGE_TYPE,
  PAGE_STATUS_LABELS,
  PAGE_TYPES,
  PAGE_TYPE_LABELS,
  RESERVED_CUSTOM_SLUGS,
  type PageStatus,
  type PageType,
  type SitePageView,
} from '@/types/site'
import { PAGE_ID_PATTERN } from '@/utils/actions'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const rawId = Number(route.params.id)
const invalidId = !Number.isInteger(rawId) || rawId <= 0
const projectId = invalidId ? 0 : rawId

const project = ref<ProjectView | null>(null)
const pages = ref<SitePageView[]>([])
const loading = ref(true)
const loadError = ref('')
const loadErrorStatus = ref(0)

const busyPageId = ref<string | null>(null)
const busyAction = ref<'publish' | 'copy' | 'delete' | 'status' | null>(null)
const publishingSite = ref(false)

/** 新建成功后提示条：可直接进入编辑器 */
const lastCreated = ref<SitePageView | null>(null)

/* ------------------------------ 加载 ------------------------------ */

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  loadErrorStatus.value = 0
  try {
    const [projectData, pageList] = await Promise.all([getProject(projectId), listPages(projectId)])
    project.value = projectData
    pages.value = pageList
  } catch (error) {
    loadError.value = errorMessageOf(error, '加载页面列表失败，请稍后重试')
    loadErrorStatus.value = isApiError(error) ? error.status : 0
  } finally {
    loading.value = false
  }
}

const publishedPageCount = computed(
  () => pages.value.filter((item) => item.status === 'PUBLISHED').length,
)

/** 已存在的模块页类型（新建时禁用重复模块页） */
const existingModuleTypes = computed<Set<PageType>>(
  () =>
    new Set(
      pages.value
        .map((item) => item.pageType)
        .filter((type) => type !== 'custom' && type !== 'home'),
    ),
)

function pageTypeLabel(type: PageType): string {
  return PAGE_TYPE_LABELS[type] ?? type
}

function statusBadgeClass(status: PageStatus): string {
  if (status === 'PUBLISHED') return 'gcc-badge-success'
  if (status === 'DISABLED') return 'gcc-badge-danger'
  return 'gcc-badge-warning'
}

/** 发布按钮是否可用（停用页/同版本已发布时禁用并给出说明） */
function publishDisabledReason(page: SitePageView): string {
  if (page.status === 'DISABLED') return '页面已停用，不能发布'
  if (page.publishedVersion !== null && page.draftVersion === page.publishedVersion) {
    return '该草稿版本已发布，修改并保存产生新版本后可再次发布'
  }
  return `发布草稿 v${page.draftVersion}（不影响其他页面）`
}

function canPublish(page: SitePageView): boolean {
  return (
    page.status !== 'DISABLED' &&
    (page.publishedVersion === null || page.draftVersion !== page.publishedVersion)
  )
}

function isBusy(page: SitePageView): boolean {
  return busyPageId.value === page.pageId
}

/* ------------------------------ 导航 ------------------------------ */

function goBack(): void {
  void router.push({ name: 'project-list' })
}

function openSettings(): void {
  void router.push({ name: 'site-settings', params: { id: projectId } })
}

function openEditor(page: SitePageView): void {
  void router.push({
    name: 'page-editor',
    params: { id: projectId, pageId: page.pageId },
  })
}

function openPreview(page: SitePageView): void {
  const href = router.resolve({
    name: 'page-preview',
    params: { id: projectId, pageId: page.pageId },
  }).href
  window.open(href, '_blank', 'noopener')
}

const CONTENT_MODULES: ReadonlyArray<{ key: string; label: string }> = [
  { key: 'changelogs', label: '更新日志' },
  { key: 'docs', label: '公开文档' },
  { key: 'team', label: '制作组' },
  { key: 'issues', label: '问题反馈' },
  { key: 'links', label: '友情链接' },
]

function openContentModule(moduleKey: string): void {
  void router.push({
    name: 'content-manager',
    params: { id: projectId, module: moduleKey },
  })
}

/* ------------------------------ 页面操作 ------------------------------ */

async function onPublishPage(page: SitePageView): Promise<void> {
  if (busyPageId.value !== null) return
  if (!canPublish(page)) {
    toast.info(publishDisabledReason(page))
    return
  }
  if (
    !window.confirm(
      `确认发布页面「${page.name}」草稿 v${page.draftVersion}？\n发布后公开页立即更新，其他页面不受影响。`,
    )
  ) {
    return
  }
  busyPageId.value = page.pageId
  busyAction.value = 'publish'
  try {
    const result = await publishPage(projectId, page.pageId)
    toast.success(`「${page.name}」发布成功：v${result.version}，公开路径 ${result.publicPath}`)
    await load()
  } catch (error) {
    if (isApiError(error) && error.status === 409) {
      toast.error(`发布失败：${error.message}`)
    } else {
      toast.error(`发布失败：${errorMessageOf(error, '请稍后重试')}`)
    }
  } finally {
    busyPageId.value = null
    busyAction.value = null
  }
}

async function onCopyPage(page: SitePageView): Promise<void> {
  if (busyPageId.value !== null) return
  busyPageId.value = page.pageId
  busyAction.value = 'copy'
  try {
    const copied = await copyPage(projectId, page.pageId)
    toast.success(`已复制为自定义页面「${copied.name}」（草稿）`)
    await load()
  } catch (error) {
    toast.error(`复制失败：${errorMessageOf(error, '请稍后重试')}`)
  } finally {
    busyPageId.value = null
    busyAction.value = null
  }
}

async function onDeletePage(page: SitePageView): Promise<void> {
  if (busyPageId.value !== null) return
  if (page.isHome) {
    toast.error('首页不能删除')
    return
  }
  const first = window.confirm(
    `确定删除页面「${page.name}」吗？\n将同时删除其草稿与全部发布快照，且无法恢复。`,
  )
  if (!first) return
  const second = window.confirm('删除是不可恢复操作，请再次确认：真的要删除这个页面吗？')
  if (!second) return

  busyPageId.value = page.pageId
  busyAction.value = 'delete'
  try {
    await deletePage(projectId, page.pageId)
    toast.success(`已删除页面「${page.name}」`)
    await load()
  } catch (error) {
    toast.error(`删除失败：${errorMessageOf(error, '请稍后重试')}`)
  } finally {
    busyPageId.value = null
    busyAction.value = null
  }
}

/** 列表内的状态快速切换：先做前端约束检查，再调用 updatePage */
async function onStatusChange(page: SitePageView, event: Event): Promise<void> {
  const select = event.target as HTMLSelectElement
  const next = select.value as PageStatus
  const previous = page.status
  if (next === previous) return

  const revert = () => {
    select.value = previous
  }
  if (next === 'PUBLISHED' && page.publishedVersion === null) {
    toast.error('该页面还没有发布过，不能切换为「已发布」；请先点击「发布」')
    revert()
    return
  }
  if (next === 'DISABLED' && page.isHome) {
    toast.error('首页不允许停用')
    revert()
    return
  }
  const confirmText =
    next === 'DISABLED'
      ? `停用页面「${page.name}」后公开端将不可见（导航中引用它的条目也会失效）。确定停用？`
      : next === 'PUBLISHED'
        ? `将页面「${page.name}」标记为已发布？公开端会显示最近一次发布快照。`
        : `将页面「${page.name}」标记为草稿？公开端将不可见（保留发布快照）。确定继续？`
  if (!window.confirm(confirmText)) {
    revert()
    return
  }

  if (busyPageId.value !== null) {
    revert()
    return
  }
  busyPageId.value = page.pageId
  busyAction.value = 'status'
  try {
    await updatePage(projectId, page.pageId, { status: next })
    toast.success(`「${page.name}」已切换为「${PAGE_STATUS_LABELS[next]}」`)
    await load()
  } catch (error) {
    revert()
    toast.error(`状态切换失败：${errorMessageOf(error, '请稍后重试')}`)
    await load()
  } finally {
    busyPageId.value = null
    busyAction.value = null
  }
}

/* ------------------------------ 站点发布 ------------------------------ */

async function onPublishSite(): Promise<void> {
  if (publishingSite.value || !project.value) return
  const ok = window.confirm(
    `确认发布站点「${project.value.name}」？\n将发布所有「非停用且草稿版本与发布版本不同」的页面，其余页面跳过。`,
  )
  if (!ok) return
  publishingSite.value = true
  try {
    const result = await publishSite(projectId)
    const published = result.publishedPages.length
    const skipped = result.skippedPages.length
    if (published > 0) {
      toast.success(`站点发布完成：发布 ${published} 个页面，跳过 ${skipped} 个页面`)
    } else {
      toast.info(`没有需要发布的页面（跳过 ${skipped} 个页面）`)
    }
    await load()
  } catch (error) {
    toast.error(`站点发布失败：${errorMessageOf(error, '请稍后重试')}`)
  } finally {
    publishingSite.value = false
  }
}

/* ------------------------------ 新建页面对话框 ------------------------------ */

const showCreate = ref(false)
const creating = ref(false)
const createError = ref('')
const createFieldErrors = ref<Record<string, string>>({})
const createForm = reactive<{ name: string; pageType: PageType; slug: string }>({
  name: '',
  pageType: 'custom',
  slug: '',
})

const slugPattern = /^[a-z0-9][a-z0-9-]{0,62}$/

/** 可新建的页面类型：home 不可创建；模块类型已被占用则禁用 */
const pageTypeOptions = computed(() =>
  PAGE_TYPES.filter((type) => type !== 'home').map((type) => {
    const occupied = type !== 'custom' && existingModuleTypes.value.has(type)
    return {
      value: type,
      label: `${pageTypeLabel(type)}${occupied ? '（已存在，不可重复创建）' : ''}`,
      disabled: occupied,
    }
  }),
)

/** 默认选中第一个可用模块类型（新站点 5 个模块页都已存在时默认 custom） */
function firstAvailablePageType(): PageType {
  const moduleTypes: PageType[] = ['changelog', 'docs', 'team', 'issue-report', 'links']
  for (const type of moduleTypes) {
    if (!existingModuleTypes.value.has(type)) return type
  }
  return 'custom'
}

const createTemplateKey = computed(() => {
  if (createForm.pageType === 'custom') return 'blank'
  return DEFAULT_TEMPLATE_BY_PAGE_TYPE[createForm.pageType] ?? ''
})

const isCustomNewPage = computed(() => createForm.pageType === 'custom')

function resetCreateForm(): void {
  createForm.pageType = firstAvailablePageType()
  createForm.name = ''
  createForm.slug = ''
  createError.value = ''
  createFieldErrors.value = {}
}

function openCreateDialog(): void {
  resetCreateForm()
  showCreate.value = true
}

function closeCreateDialog(): void {
  if (creating.value) return
  showCreate.value = false
}

function onNewNameInput(event: Event): void {
  createForm.name = (event.target as HTMLInputElement).value
}

function onNewPageTypeChange(event: Event): void {
  createForm.pageType = (event.target as HTMLSelectElement).value as PageType
  createFieldErrors.value = {}
  createError.value = ''
}

function onNewSlugInput(event: Event): void {
  createForm.slug = (event.target as HTMLInputElement).value
    .toLowerCase()
    .replace(/[^a-z0-9-]/g, '')
    .replace(/^-+/, '')
}

function validateNewPage(): boolean {
  const errors: Record<string, string> = {}
  const name = createForm.name.trim()
  if (name.length < 1) errors.name = '请填写页面名称'
  else if (name.length > 40) errors.name = '页面名称不能超过 40 个字符'

  if (createForm.pageType !== 'custom' && existingModuleTypes.value.has(createForm.pageType)) {
    errors.pageType = `「${pageTypeLabel(createForm.pageType)}」页面已存在，每个模块类型只能有一个页面`
  }
  if (isCustomNewPage.value) {
    const slug = createForm.slug.trim()
    if (!slug) errors.slug = '自定义页面需要填写 slug'
    else if (!slugPattern.test(slug)) {
      errors.slug = 'slug 需为 1-63 位小写字母、数字或中划线，且以字母或数字开头'
    } else if (RESERVED_CUSTOM_SLUGS.includes(slug)) {
      errors.slug = `slug「${slug}」是系统保留字，请更换`
    } else if (pages.value.some((item) => item.slug === slug)) {
      errors.slug = `slug「${slug}」已被本站其他页面使用，请更换`
    }
  }
  createFieldErrors.value = errors
  return Object.keys(errors).length === 0
}

async function submitCreatePage(): Promise<void> {
  if (creating.value) return
  createError.value = ''
  if (!validateNewPage()) return
  creating.value = true
  try {
    const created = await createPage(projectId, {
      name: createForm.name.trim(),
      pageType: createForm.pageType,
      slug: isCustomNewPage.value ? createForm.slug.trim() : undefined,
      templateKey: createTemplateKey.value || undefined,
    })
    toast.success(`页面「${created.name}」创建成功，已生成默认草稿`)
    lastCreated.value = created
    showCreate.value = false
    await load()
  } catch (error) {
    if (isApiError(error)) {
      createFieldErrors.value = { ...(error.fieldErrors ?? {}) }
      if (Object.keys(createFieldErrors.value).length === 0) {
        createError.value = error.message || '创建失败，请检查填写内容'
      }
      if (error.status === 409) {
        createError.value = error.message || '页面已存在或 slug 冲突，请更换'
      }
      toast.error(error.message || '创建页面失败')
    } else {
      createError.value = errorMessageOf(error, '创建页面失败，请稍后重试')
      toast.error(createError.value)
    }
  } finally {
    creating.value = false
  }
}

/* ------------------------------ 编辑元信息对话框 ------------------------------ */

const showEdit = ref(false)
const editing = ref(false)
const editError = ref('')
const editFieldErrors = ref<Record<string, string>>({})
const editTarget = ref<SitePageView | null>(null)
const editForm = reactive<{ name: string; slug: string; status: PageStatus }>({
  name: '',
  slug: '',
  status: 'DRAFT',
})

const canEditSlug = computed(() => editTarget.value?.pageType === 'custom')

function openEditDialog(page: SitePageView): void {
  editTarget.value = page
  editForm.name = page.name
  editForm.slug = page.slug
  editForm.status = page.status
  editError.value = ''
  editFieldErrors.value = {}
  showEdit.value = true
}

function closeEditDialog(): void {
  if (editing.value) return
  showEdit.value = false
}

function onEditNameInput(event: Event): void {
  editForm.name = (event.target as HTMLInputElement).value
}

function onEditSlugInput(event: Event): void {
  editForm.slug = (event.target as HTMLInputElement).value
    .toLowerCase()
    .replace(/[^a-z0-9-]/g, '')
    .replace(/^-+/, '')
}

function onEditStatusChange(event: Event): void {
  editForm.status = (event.target as HTMLSelectElement).value as PageStatus
}

/** 状态下拉里各选项的禁用条件（与后端校验保持一致，提前在前端提示） */
function editStatusOptionDisabled(status: PageStatus): boolean {
  if (!editTarget.value) return false
  if (status === editForm.status) return false
  if (status === 'DISABLED' && editTarget.value.isHome) return true
  if (status === 'PUBLISHED' && editTarget.value.publishedVersion === null) return true
  return false
}

function validateEditPage(): boolean {
  if (!editTarget.value) return false
  const errors: Record<string, string> = {}
  const name = editForm.name.trim()
  if (name.length < 1) errors.name = '请填写页面名称'
  else if (name.length > 40) errors.name = '页面名称不能超过 40 个字符'

  if (canEditSlug.value) {
    const slug = editForm.slug.trim()
    if (!slug) errors.slug = '自定义页面 slug 不能为空'
    else if (!slugPattern.test(slug)) {
      errors.slug = 'slug 需为 1-63 位小写字母、数字或中划线，且以字母或数字开头'
    } else if (RESERVED_CUSTOM_SLUGS.includes(slug)) {
      errors.slug = `slug「${slug}」是系统保留字，请更换`
    } else if (
      pages.value.some((item) => item.slug === slug && item.pageId !== editTarget.value?.pageId)
    ) {
      errors.slug = `slug「${slug}」已被本站其他页面使用，请更换`
    }
  }
  if (editForm.status === 'PUBLISHED' && editTarget.value.publishedVersion === null) {
    errors.status = '该页面还没有发布过，不能切换为「已发布」；请先发布页面'
  }
  if (editForm.status === 'DISABLED' && editTarget.value.isHome) {
    errors.status = '首页不允许停用'
  }
  editFieldErrors.value = errors
  return Object.keys(errors).length === 0
}

async function submitEditPage(): Promise<void> {
  if (editing.value || !editTarget.value) return
  editError.value = ''
  if (!validateEditPage()) return
  const target = editTarget.value
  editing.value = true
  try {
    const payload: { name: string; status: PageStatus; slug?: string } = {
      name: editForm.name.trim(),
      status: editForm.status,
    }
    if (canEditSlug.value) payload.slug = editForm.slug.trim()
    await updatePage(projectId, target.pageId, payload)
    toast.success(`页面「${editForm.name.trim()}」信息已更新`)
    showEdit.value = false
    await load()
  } catch (error) {
    if (isApiError(error)) {
      editFieldErrors.value = { ...(error.fieldErrors ?? {}) }
      if (Object.keys(editFieldErrors.value).length === 0) {
        editError.value = error.message || '保存失败，请检查填写内容'
      }
      toast.error(error.message || '保存失败')
    } else {
      editError.value = errorMessageOf(error, '保存失败，请稍后重试')
      toast.error(editError.value)
    }
  } finally {
    editing.value = false
  }
}

/* ------------------------------ 生命周期 ------------------------------ */

onMounted(() => {
  if (invalidId) {
    loading.value = false
    loadError.value = '地址中的站点 ID 不合法，请从站点列表进入'
    loadErrorStatus.value = 0
    return
  }
  void load()
})

/** 页面 ID 合法时才能进入编辑器（防御脏数据） */
function validPageId(page: SitePageView): boolean {
  return PAGE_ID_PATTERN.test(page.pageId)
}
</script>

<template>
  <div class="pages-page x-page gcc-container">
    <ToastHost />

    <PageHeader kicker="Pages" :title="project ? `页面管理 · ${project.name}` : '页面管理'">
      <template #actions>
        <button type="button" class="gcc-btn gcc-btn-sm gcc-btn-ghost" @click="goBack">
          ← 站点列表
        </button>
        <button type="button" class="gcc-btn gcc-btn-sm" @click="openSettings">站点设置</button>
        <button
          type="button"
          class="gcc-btn gcc-btn-sm gcc-btn-primary"
          :disabled="publishingSite || !project"
          title="发布所有有更新的页面"
          @click="onPublishSite"
        >
          <XIcon name="publish" :size="13" />
          {{ publishingSite ? '发布中…' : '站点发布' }}
        </button>
      </template>
    </PageHeader>

    <!-- 站点上下文 / 统计 -->
    <div class="pages-meta">
      <span v-if="project" class="x-meta">
        GAME / {{ project.gameName }} · SLUG /{{ project.slug }}
      </span>
      <span
        v-if="project"
        class="gcc-badge"
        :class="project.status === 'PUBLISHED' ? 'gcc-badge-success' : 'gcc-badge-warning'"
      >
        {{ project.status === 'PUBLISHED' ? '已发布站点' : '未发布站点' }}
      </span>
      <span class="gcc-spacer" />
      <span class="x-meta pages-meta__counts">
        {{ pages.length }} PAGES · {{ publishedPageCount }} PUBLISHED ·
        {{ pages.length - publishedPageCount }} DRAFTS
      </span>
    </div>

    <!-- 加载中 -->
    <section v-if="loading" class="state-block" aria-busy="true">
      <div v-for="index in 6" :key="index" class="gcc-skeleton skeleton-row" />
    </section>

    <!-- 加载失败 -->
    <section v-else-if="loadError" class="gcc-error state-block">
      <p>{{ loadError }}</p>
      <div class="state-actions">
        <button type="button" class="gcc-btn" @click="load">重试</button>
        <button type="button" class="gcc-btn gcc-btn-ghost" @click="goBack">返回站点列表</button>
      </div>
    </section>

    <!-- 页面列表 -->
    <template v-else>
      <div class="pages-toolbar">
        <nav class="content-links" aria-label="内容管理快捷入口">
          <span class="links-label x-kicker">Content</span>
          <button
            v-for="module in CONTENT_MODULES"
            :key="module.key"
            type="button"
            class="content-link"
            @click="openContentModule(module.key)"
          >
            {{ module.label }}
          </button>
        </nav>
        <span class="gcc-spacer" />
        <button type="button" class="gcc-btn gcc-btn-sm gcc-btn-primary" @click="openCreateDialog">
          <XIcon name="plus" :size="14" />
          新建页面
        </button>
      </div>

      <!-- 新建成功提示 -->
      <section v-if="lastCreated" class="created-banner">
        <span class="created-banner__text">
          页面「{{ lastCreated.name }}」（{{ pageTypeLabel(lastCreated.pageType) }}）已创建，草稿已按模板初始化。
        </span>
        <span class="gcc-spacer" />
        <button
          type="button"
          class="gcc-btn gcc-btn-sm gcc-btn-primary"
          @click="openEditor(lastCreated)"
        >
          进入编辑器
        </button>
        <RouterLink
          class="gcc-btn gcc-btn-sm"
          :to="{ name: 'page-preview', params: { id: projectId, pageId: lastCreated.pageId } }"
          target="_blank"
        >
          草稿预览
        </RouterLink>
        <button type="button" class="gcc-btn gcc-btn-sm gcc-btn-ghost" @click="lastCreated = null">
          关闭
        </button>
      </section>

      <!-- 空列表（理论上新建站点至少有首页，防御异常数据） -->
      <section v-if="pages.length === 0" class="gcc-empty state-block">
        <p class="x-kicker">NO PAGES</p>
        <h2 class="x-title">该站点还没有页面</h2>
        <p class="gcc-muted">新建一个自定义页面开始搭建。</p>
        <button type="button" class="gcc-btn gcc-btn-primary" @click="openCreateDialog">
          新建页面
        </button>
      </section>

      <section v-else class="page-table" role="table" aria-label="页面列表">
        <div class="table-head" role="row">
          <span class="x-kicker" role="columnheader">页面</span>
          <span class="x-kicker" role="columnheader">类型</span>
          <span class="x-kicker" role="columnheader">状态</span>
          <span class="x-kicker" role="columnheader">草稿 / 发布</span>
          <span class="x-kicker" role="columnheader">更新时间</span>
          <span class="x-kicker" role="columnheader">操作</span>
        </div>

        <div
          v-for="page in pages"
          :key="page.pageId"
          class="table-row"
          :class="{ 'is-home': page.isHome, 'is-disabled': page.status === 'DISABLED' }"
          role="row"
        >
          <div class="cell page-cell" role="cell">
            <div class="page-name-line">
              <strong class="page-name" :title="page.name">{{ page.name }}</strong>
              <span v-if="page.isHome" class="gcc-badge gcc-badge-primary">首页</span>
            </div>
            <span class="page-slug x-mono">/{{ page.slug }}</span>
            <span class="page-id x-mono" :title="page.pageId">{{ page.pageId }}</span>
          </div>

          <div class="cell" role="cell">
            <span class="type-text x-mono">{{ pageTypeLabel(page.pageType) }}</span>
            <span v-if="page.templateKey" class="template-text x-mono">{{ page.templateKey }}</span>
          </div>

          <div class="cell" role="cell">
            <span class="gcc-badge" :class="statusBadgeClass(page.status)">
              {{ PAGE_STATUS_LABELS[page.status] }}
            </span>
          </div>

          <div class="cell version-cell" role="cell">
            <span class="version-chip x-mono">草稿 v{{ page.draftVersion }}</span>
            <span class="version-chip x-mono" :class="{ published: page.publishedVersion !== null }">
              {{ page.publishedVersion === null ? '未发布' : `发布 v${page.publishedVersion}` }}
            </span>
          </div>

          <div class="cell time-cell x-mono" role="cell">{{ formatDateTime(page.updatedAt) }}</div>

          <div class="cell actions-cell" role="cell">
            <button
              type="button"
              class="gcc-btn gcc-btn-sm"
              :disabled="!validPageId(page)"
              title="打开三栏可视化编辑器"
              @click="openEditor(page)"
            >
              编辑器
            </button>
            <button
              type="button"
              class="gcc-btn gcc-btn-sm"
              title="在新窗口预览草稿"
              @click="openPreview(page)"
            >
              预览
            </button>
            <button
              type="button"
              class="gcc-btn gcc-btn-sm"
              :disabled="busyPageId !== null || !canPublish(page)"
              :title="publishDisabledReason(page)"
              @click="onPublishPage(page)"
            >
              {{ isBusy(page) && busyAction === 'publish' ? '发布中…' : '发布' }}
            </button>
            <button
              type="button"
              class="gcc-btn gcc-btn-sm"
              :disabled="busyPageId !== null"
              title="复制该页面为新的自定义页面（含草稿）"
              @click="onCopyPage(page)"
            >
              {{ isBusy(page) && busyAction === 'copy' ? '复制中…' : '复制' }}
            </button>
            <button
              type="button"
              class="gcc-btn gcc-btn-sm"
              :disabled="busyPageId !== null"
              title="编辑页面名称 / slug / 状态"
              @click="openEditDialog(page)"
            >
              编辑信息
            </button>
            <select
              class="gcc-select status-select x-mono"
              :value="page.status"
              :disabled="busyPageId !== null"
              aria-label="切换页面状态"
              title="切换页面状态"
              @change="onStatusChange(page, $event)"
            >
              <option value="DRAFT">草稿</option>
              <option
                value="PUBLISHED"
                :disabled="page.publishedVersion === null"
                :title="page.publishedVersion === null ? '请先发布该页面' : ''"
              >
                已发布
              </option>
              <option
                value="DISABLED"
                :disabled="page.isHome"
                :title="page.isHome ? '首页不允许停用' : ''"
              >
                已停用
              </option>
            </select>
            <button
              type="button"
              class="gcc-btn gcc-btn-sm gcc-btn-danger"
              :disabled="busyPageId !== null || page.isHome"
              :title="page.isHome ? '首页不能删除' : '删除页面（含草稿与发布快照）'"
              @click="onDeletePage(page)"
            >
              {{ isBusy(page) && busyAction === 'delete' ? '删除中…' : '删除' }}
            </button>
          </div>
        </div>
      </section>
    </template>

    <!-- ============================ 新建页面对话框 ============================ -->
    <div v-if="showCreate" class="modal-mask" @click.self="closeCreateDialog">
      <div class="modal-card" role="dialog" aria-modal="true" aria-label="新建页面">
        <header class="modal-head">
          <div>
            <p class="x-kicker">New page</p>
            <h2 class="modal-title">新建页面</h2>
          </div>
          <button type="button" class="modal-close" aria-label="关闭" @click="closeCreateDialog">×</button>
        </header>

        <div v-if="createError" class="gcc-error modal-error">{{ createError }}</div>

        <div class="modal-body">
          <div class="gcc-field">
            <label class="gcc-label" for="new-page-name">页面名称 <span class="required">*</span></label>
            <input
              id="new-page-name"
              class="gcc-input"
              type="text"
              maxlength="40"
              :value="createForm.name"
              placeholder="例如：玩法指南"
              @input="onNewNameInput"
            />
            <div class="field-meta">
              <span class="field-error">{{ createFieldErrors.name ?? '' }}</span>
              <span class="x-mono">{{ createForm.name.length }}/40</span>
            </div>
          </div>

          <div class="gcc-field">
            <label class="gcc-label" for="new-page-type">页面类型 <span class="required">*</span></label>
            <select
              id="new-page-type"
              class="gcc-select"
              :value="createForm.pageType"
              @change="onNewPageTypeChange"
            >
              <option
                v-for="option in pageTypeOptions"
                :key="option.value"
                :value="option.value"
                :disabled="option.disabled"
              >
                {{ option.label }}
              </option>
            </select>
            <p class="field-help">
              首页由系统创建不可新增；每个模块类型（更新日志/文档/制作组/问题反馈/友情链接）每站最多一个；
              自定义页面可以创建任意多个。
            </p>
            <p v-if="createFieldErrors.pageType" class="field-error">{{ createFieldErrors.pageType }}</p>
          </div>

          <div v-if="isCustomNewPage" class="gcc-field">
            <label class="gcc-label" for="new-page-slug">页面 slug <span class="required">*</span></label>
            <input
              id="new-page-slug"
              class="gcc-input x-mono"
              type="text"
              maxlength="63"
              :value="createForm.slug"
              placeholder="guides"
              @input="onNewSlugInput"
            />
            <div class="field-meta">
              <span class="field-error">{{ createFieldErrors.slug ?? '' }}</span>
            </div>
            <p class="field-help">
              1-63 位小写字母、数字或中划线；公开地址
              <span class="x-mono"
                >/community/{{ project?.slug ?? '' }}/p/{{ createForm.slug || 'your-slug' }}</span
              >；保留字（home / p / changelog / docs / team / report / links / api / admin）不可用。
            </p>
          </div>

        </div>

        <footer class="modal-actions">
          <button
            type="button"
            class="gcc-btn gcc-btn-ghost"
            :disabled="creating"
            @click="closeCreateDialog"
          >
            取消
          </button>
          <button
            type="button"
            class="gcc-btn gcc-btn-primary"
            :disabled="creating"
            @click="submitCreatePage"
          >
            {{ creating ? '创建中…' : '创建页面' }}
          </button>
        </footer>
      </div>
    </div>

    <!-- ============================ 编辑元信息对话框 ============================ -->
    <div v-if="showEdit && editTarget" class="modal-mask" @click.self="closeEditDialog">
      <div class="modal-card" role="dialog" aria-modal="true" aria-label="编辑页面信息">
        <header class="modal-head">
          <div>
            <p class="x-kicker">Edit page</p>
            <h2 class="modal-title">编辑页面信息</h2>
          </div>
          <button type="button" class="modal-close" aria-label="关闭" @click="closeEditDialog">×</button>
        </header>

        <div v-if="editError" class="gcc-error modal-error">{{ editError }}</div>

        <div class="modal-body">
          <div class="gcc-field">
            <label class="gcc-label" for="edit-page-name">页面名称 <span class="required">*</span></label>
            <input
              id="edit-page-name"
              class="gcc-input"
              type="text"
              maxlength="40"
              :value="editForm.name"
              @input="onEditNameInput"
            />
            <div class="field-meta">
              <span class="field-error">{{ editFieldErrors.name ?? '' }}</span>
              <span class="x-mono">{{ editForm.name.length }}/40</span>
            </div>
          </div>

          <div class="gcc-field">
            <label class="gcc-label" for="edit-page-slug">页面 slug</label>
            <input
              v-if="canEditSlug"
              id="edit-page-slug"
              class="gcc-input x-mono"
              type="text"
              maxlength="63"
              :value="editForm.slug"
              @input="onEditSlugInput"
            />
            <div v-else class="readonly-box x-mono">{{ editTarget.slug }}</div>
            <p class="field-help">
              {{
                canEditSlug
                  ? '修改 slug 不影响按钮跳转（按钮基于稳定 pageId），但公开页 URL 会变化。'
                  : '首页与模块页面的 slug 由系统固定，不可修改。'
              }}
            </p>
            <p v-if="editFieldErrors.slug" class="field-error">{{ editFieldErrors.slug }}</p>
          </div>

          <div class="gcc-field">
            <label class="gcc-label" for="edit-page-status">页面状态</label>
            <select
              id="edit-page-status"
              class="gcc-select"
              :value="editForm.status"
              @change="onEditStatusChange"
            >
              <option value="DRAFT">草稿</option>
              <option
                value="PUBLISHED"
                :disabled="editStatusOptionDisabled('PUBLISHED')"
                :title="editTarget.publishedVersion === null ? '该页面还没有发布过，请先发布' : ''"
              >
                已发布
              </option>
              <option
                value="DISABLED"
                :disabled="editStatusOptionDisabled('DISABLED')"
                :title="editTarget.isHome ? '首页不允许停用' : ''"
              >
                已停用
              </option>
            </select>
            <p v-if="editTarget.isHome" class="field-help">首页不允许停用，也不能删除。</p>
            <p v-if="editFieldErrors.status" class="field-error">{{ editFieldErrors.status }}</p>
          </div>

          <div class="gcc-field">
            <label class="gcc-label">页面 ID（只读）</label>
            <div class="readonly-box x-mono">{{ editTarget.pageId }}</div>
          </div>
        </div>

        <footer class="modal-actions">
          <button
            type="button"
            class="gcc-btn gcc-btn-ghost"
            :disabled="editing"
            @click="closeEditDialog"
          >
            取消
          </button>
          <button
            type="button"
            class="gcc-btn gcc-btn-primary"
            :disabled="editing"
            @click="submitEditPage"
          >
            {{ editing ? '保存中…' : '保存' }}
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pages-page {
  --page-danger-fg: var(--x-danger);
  --page-warning-fg: var(--x-warning);
  padding-top: 36px;
}

/* ---------------- meta strip ---------------- */
.pages-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--x-border);
}

.pages-meta__counts {
  font-size: 10px;
  letter-spacing: 0.12em;
}

/* ---------------- toolbar ---------------- */
.pages-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.content-links {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.links-label {
  margin-right: 4px;
  font-size: 9.5px;
}

.content-link {
  padding: 4px 12px;
  border-radius: var(--x-radius-pill);
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

.content-link:hover {
  color: var(--x-text);
  border-color: var(--x-border-strong);
  background: var(--x-surface-2);
}

.state-block {
  margin-top: 8px;
}

.state-actions {
  display: flex;
  gap: 10px;
}

.skeleton-row {
  height: 56px;
  margin-bottom: 8px;
}

.created-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding: 12px 16px;
  border: 1px solid rgba(61, 220, 151, 0.3);
  border-radius: var(--x-radius);
  background: rgba(61, 220, 151, 0.06);
  font-size: 13px;
  color: var(--x-text-secondary);
}

.created-banner__text {
  color: var(--x-text);
}

/* ---------------- table ---------------- */
.page-table {
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  overflow: hidden;
  background: var(--x-surface);
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: minmax(190px, 1.35fr) 132px 104px 156px 130px minmax(380px, 2.3fr);
  gap: 14px;
  align-items: center;
  padding: 12px 18px;
}

.table-head {
  border-bottom: 1px solid var(--x-border);
  background: var(--x-canvas-soft);
}

.table-head .x-kicker {
  font-size: 9px;
}

.table-row {
  border-top: 1px solid var(--x-border);
  transition: background 0.12s ease;
}

.table-row:first-of-type {
  border-top: none;
}

.table-row:hover {
  background: var(--x-surface-2);
}

.table-row.is-disabled {
  opacity: 0.6;
}

.table-row.is-home {
  background: rgba(255, 122, 23, 0.02);
}

.cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.page-name-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.page-name {
  font-size: 13.5px;
  font-weight: 500;
  letter-spacing: -0.005em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-slug {
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--x-muted);
}

.page-id {
  font-size: 9px;
  letter-spacing: 0.04em;
  color: var(--x-muted);
  opacity: 0.65;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-text {
  font-size: 11.5px;
  letter-spacing: 0.02em;
  color: var(--x-text-secondary);
}

.template-text {
  font-size: 9.5px;
  letter-spacing: 0.06em;
  color: var(--x-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-cell {
  flex-direction: row;
  flex-wrap: wrap;
  gap: 5px;
}

.version-chip {
  padding: 2px 8px;
  border-radius: var(--x-radius-pill);
  border: 1px solid var(--x-border);
  background: transparent;
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: var(--x-muted);
  white-space: nowrap;
}

.version-chip.published {
  color: var(--x-text-secondary);
  border-color: var(--x-border-strong);
}

.time-cell {
  font-size: 10.5px;
  letter-spacing: 0.04em;
  color: var(--x-muted);
}

.actions-cell {
  flex-direction: row;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.actions-cell .gcc-btn {
  padding: 4px 11px;
  font-size: 11.5px;
}

.status-select {
  width: 98px;
  padding: 4px 8px;
  border-radius: var(--x-radius-pill);
  font-size: 11px;
}

/* ---------------- dialog ---------------- */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--gcc-mask);
}

.modal-card {
  width: min(560px, 100%);
  max-height: min(86vh, 780px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--x-border-strong);
  border-radius: var(--x-radius);
  background: var(--x-surface-2);
}

.modal-head {
  flex: none;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 22px 24px 16px;
  border-bottom: 1px solid var(--x-border);
}

.modal-title {
  margin-top: 6px;
  font-size: 20px;
  font-weight: 400;
  letter-spacing: -0.02em;
}

.modal-close {
  flex: none;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--x-border);
  border-radius: 50%;
  background: transparent;
  color: var(--x-muted);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}

.modal-close:hover {
  color: var(--x-text);
  border-color: var(--x-border-strong);
}

.modal-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.modal-error {
  margin: 14px 24px 0;
}

.modal-actions {
  flex: none;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--x-border);
}

.field-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 10px;
  letter-spacing: 0.03em;
  color: var(--x-muted);
}

.field-error {
  font-size: 11px;
  color: var(--page-danger-fg);
}

.field-help {
  font-size: 11px;
  line-height: 1.65;
  color: var(--x-muted);
}

.required {
  color: var(--x-danger);
}

.template-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px dashed var(--x-border-strong);
  border-radius: var(--x-radius);
  background: var(--x-canvas-soft);
  font-size: 12px;
  line-height: 1.6;
}

.readonly-box {
  padding: 10px 12px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--gcc-input-bg);
  color: var(--x-muted);
  font-size: 12px;
}

:global(.admin-theme-scope[data-admin-theme='light'] .pages-page) {
  --page-danger-fg: #b42318;
  --page-warning-fg: #8a5a00;
}

/* 1280px 内可用：窄屏表格退化为单列堆叠 */
@media (max-width: 1240px) {
  .table-head {
    display: none;
  }

  .table-row {
    grid-template-columns: 1fr;
    gap: 8px;
    padding: 14px 16px;
  }
}
</style>
