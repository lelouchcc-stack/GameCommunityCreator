/**
 * 页面级可视化编辑器状态（Vue ref/computed；编辑器是页面级实例，不放进 Pinia 全局 store）。
 *
 * 与 v1 的区别：编辑器不再绑定「项目 = 单页」，而是绑定「项目 + 页面」：
 * - 加载：getProject + getPage + listPages（动作目标/页面概览）+ getPageDraft + getSiteSettings
 * - 保存：savePageDraft(projectId, pageId, config, expectedVersion)（草稿级乐观锁）
 * - 发布：publishPage(projectId, pageId)（页面级发布，不影响其他页面）
 * - 重置：resetPageDraft(projectId, pageId)（按页面 templateKey）
 * - 校验：validateDraftConfig(config, { projectSlug, pageId, targetPages }) —— errors 阻止保存，warnings 允许保存但提示
 *
 * 视图层（EditorView.vue）只负责布局、确认框与快捷键，不重复这些逻辑。
 */
import { computed, ref } from 'vue'
import { isApiError } from '@/api/client'
import { getProject } from '@/api/projects'
import {
  getPage,
  getPageDraft,
  publishPage,
  resetPageDraft,
  savePageDraft,
  listPages,
} from '@/api/pages'
import { getSiteSettings } from '@/api/site'
import {
  MAX_COMPONENTS,
  MIN_COMPONENTS,
  type ComponentPropValue,
  type ComponentType,
  type PageAction,
  type PageComponent,
  type PageConfig,
  type ThemeConfig,
} from '@/types/page'
import type { ProjectView } from '@/types/api'
import type {
  PublishPageResult,
  SitePageDraftEnvelope,
  SitePageView,
  SiteRuntimeContext,
  SiteSettings,
} from '@/types/site'
import { PAGE_STATUS_LABELS, PAGE_TYPE_LABELS } from '@/types/site'
import {
  cloneConfig,
  createComponent,
  findComponent,
  validateDraftConfig,
} from '@/utils/pageConfig'
import { useToast } from './useToast'

/** 把未知异常转成用户可读的中文消息（ApiError.message 已是中文） */
export function errorMessageOf(error: unknown, fallback: string): string {
  if (isApiError(error)) return error.message || fallback
  if (error instanceof Error) return error.message || fallback
  return fallback
}

/** 把字段错误对象拍平成 "字段路径：原因" 列表（展示在顶部告警条） */
export function fieldErrorList(fieldErrors: Record<string, string>): string[] {
  return Object.entries(fieldErrors).map(([key, message]) => `${key}：${message}`)
}

function serializeConfig(config: PageConfig): string {
  return JSON.stringify(config)
}

/** 动作对象重建：切换类型时清掉无关字段，保证只写协议允许的键 */
export function normalizeAction(action: PageAction): PageAction {
  if (action.type === 'navigate') {
    return { type: 'navigate', targetPageId: action.targetPageId ?? '' }
  }
  if (action.type === 'externalLink') {
    return {
      type: 'externalLink',
      url: action.url ?? '',
      openInNewTab: action.openInNewTab !== false,
    }
  }
  return { type: 'none' }
}

/** 加载阶段发现的可自动修复的小问题（例如配置里的 pageId 串页），展示给用户 */
export interface LoadWarning {
  message: string
}

export interface SaveResult {
  ok: boolean
  /** 409：配置已被其他操作修改 */
  conflict?: boolean
  /** 400：后端校验失败 */
  fieldErrors?: Record<string, string>
}

export function useEditorState(projectId: number, pageId: string) {
  const toast = useToast()

  const project = ref<ProjectView | null>(null)
  const page = ref<SitePageView | null>(null)
  /** 站点全部页面：按钮动作目标下拉 + 导航/页面概览用 */
  const pages = ref<SitePageView[]>([])
  const settings = ref<SiteSettings | null>(null)

  const config = ref<PageConfig | null>(null)
  const configVersion = ref(0)
  const lastSavedAt = ref('')
  const savedSnapshot = ref('')
  const loadWarnings = ref<string[]>([])

  const selectedId = ref<string | null>(null)

  const loading = ref(true)
  const loadError = ref('')
  const loadErrorStatus = ref(0)

  const saving = ref(false)
  const publishing = ref(false)
  const resetting = ref(false)

  const saveError = ref('')
  const conflict = ref(false)
  const fieldErrors = ref<Record<string, string>>({})
  const validationErrors = ref<string[]>([])
  const validationWarnings = ref<string[]>([])

  const selectedComponent = computed<PageComponent | null>(() => {
    if (!config.value || !selectedId.value) return null
    return findComponent(config.value, selectedId.value) ?? null
  })

  /** 本地配置与"最后一次成功保存/加载"的快照不一致 → 有未保存修改 */
  const dirty = computed(() => {
    if (!config.value) return false
    return serializeConfig(config.value) !== savedSnapshot.value
  })

  /** 草稿版本是否还有未发布的内容（决定"发布"按钮是否可用） */
  const hasUnpublishedVersion = computed(() => {
    if (!page.value) return false
    if (page.value.status === 'DISABLED') return false
    if (page.value.publishedVersion === null) return true
    return page.value.draftVersion !== page.value.publishedVersion
  })

  const pageTypeLabel = computed(() =>
    page.value ? (PAGE_TYPE_LABELS[page.value.pageType] ?? page.value.pageType) : '',
  )
  const pageStatusLabel = computed(() =>
    page.value ? (PAGE_STATUS_LABELS[page.value.status] ?? page.value.status) : '',
  )

  /** 传给 CommunityRuntime 的站点上下文（动作解析 / 主题 / 页面注册表） */
  const siteContext = computed<SiteRuntimeContext | null>(() => {
    if (!project.value) return null
    return {
      projectId: project.value.id,
      projectSlug: project.value.slug,
      pages: pages.value.map((item) => ({
        pageId: item.pageId,
        name: item.name,
        slug: item.slug,
        pageType: item.pageType,
        status: item.status,
      })),
      themeMode: settings.value?.themeMode ?? 'DARK',
    }
  })

  /** 当前选中组件的索引（从 1 开始，0 表示没有选中） */
  const selectedIndex = computed(() => {
    if (!config.value || !selectedId.value) return 0
    return config.value.components.findIndex((item) => item.id === selectedId.value) + 1
  })

  function clearMessages(): void {
    saveError.value = ''
    conflict.value = false
    fieldErrors.value = {}
    validationErrors.value = []
    validationWarnings.value = []
    loadWarnings.value = []
  }

  /**
   * 应用服务器返回的页面草稿。
   * 配置里 pageId / projectSlug 与当前页面不一致时（历史数据或复制残留），
   * 本地方正并提示，用户保存后写入正确值（后端仍会做权威校验）。
   */
  function applyPageEnvelope(envelope: SitePageDraftEnvelope): void {
    const next = cloneConfig(envelope.config)
    const warnings: string[] = []
    const expectedPageId = pageId
    const expectedSlug = project.value?.slug ?? ''
    if (next.pageId !== expectedPageId) {
      warnings.push(
        `配置中的 pageId（${String(next.pageId)}）与当前页面不一致，已在本地方正，保存后修正。`,
      )
      next.pageId = expectedPageId
    }
    if (expectedSlug && next.projectSlug !== expectedSlug) {
      warnings.push(
        `配置中的项目标识（${String(next.projectSlug)}）与当前站点不一致，已在本地方正，保存后修正。`,
      )
      next.projectSlug = expectedSlug
    }
    if (next.schemaVersion !== 2) {
      warnings.push(
        `该页草稿仍是 schemaVersion=${String(next.schemaVersion)} 的旧格式，保存时会升级为 v2。`,
      )
      next.schemaVersion = 2
    }

    configVersion.value = envelope.configVersion
    config.value = next
    savedSnapshot.value = serializeConfig(next)
    lastSavedAt.value = envelope.updatedAt
    conflict.value = false
    saveError.value = ''
    fieldErrors.value = {}
    validationErrors.value = []
    validationWarnings.value = []
    loadWarnings.value = warnings
    if (page.value) {
      page.value = {
        ...page.value,
        draftVersion: envelope.configVersion,
        updatedAt: envelope.updatedAt,
        slug: envelope.pageSlug || page.value.slug,
      }
    }
    if (selectedId.value && !next.components.some((item) => item.id === selectedId.value)) {
      selectedId.value = null
    }
  }

  /** 加载项目、页面元信息、全部页面列表、页面草稿与站点设置 */
  async function load(): Promise<void> {
    loading.value = true
    loadError.value = ''
    loadErrorStatus.value = 0
    try {
      const [projectData, pageData, pagesData, draft, siteSettings] = await Promise.all([
        getProject(projectId),
        getPage(projectId, pageId),
        listPages(projectId),
        getPageDraft(projectId, pageId),
        getSiteSettings(projectId),
      ])
      project.value = projectData
      page.value = pageData
      pages.value = pagesData
      settings.value = siteSettings
      applyPageEnvelope(draft)
    } catch (error) {
      loadError.value = errorMessageOf(error, '编辑器加载失败，请稍后重试')
      loadErrorStatus.value = isApiError(error) ? error.status : 0
    } finally {
      loading.value = false
    }
  }

  /** 冲突后刷新：丢弃本地未保存修改，重新读取服务器最新草稿 */
  async function reloadFromServer(): Promise<void> {
    loading.value = true
    loadError.value = ''
    try {
      const [projectData, pageData, pagesData, draft, siteSettings] = await Promise.all([
        getProject(projectId),
        getPage(projectId, pageId),
        listPages(projectId),
        getPageDraft(projectId, pageId),
        getSiteSettings(projectId),
      ])
      project.value = projectData
      page.value = pageData
      pages.value = pagesData
      settings.value = siteSettings
      applyPageEnvelope(draft)
      toast.success('已刷新为服务器上的最新配置')
    } catch (error) {
      toast.error(`刷新失败：${errorMessageOf(error, '请稍后重试')}`)
    } finally {
      loading.value = false
    }
  }

  /** 仅刷新站点页面列表（发布/删除页面后同步动作目标下拉） */
  async function refreshPages(): Promise<void> {
    try {
      pages.value = await listPages(projectId)
    } catch {
      /* 页面列表刷新失败不影响主流程，保持旧数据 */
    }
  }

  /* ---------------- 选中 ---------------- */
  function select(id: string | null): void {
    selectedId.value = id
  }

  /* ---------------- 组件增删与排序 ---------------- */
  function addComponent(type: ComponentType): void {
    if (!config.value) return
    if (config.value.components.length >= MAX_COMPONENTS) {
      toast.error(`最多只能添加 ${MAX_COMPONENTS} 个组件，请先删除其他组件`)
      return
    }
    const component = createComponent(type)
    config.value.components.push(component)
    selectedId.value = component.id
  }

  function removeComponent(id: string): void {
    if (!config.value) return
    if (config.value.components.length <= MIN_COMPONENTS) {
      toast.error(`页面至少保留 ${MIN_COMPONENTS} 个组件`)
      return
    }
    const index = config.value.components.findIndex((item) => item.id === id)
    if (index < 0) return
    config.value.components.splice(index, 1)
    if (selectedId.value === id) {
      const nextIndex = Math.min(index, config.value.components.length - 1)
      selectedId.value = config.value.components[nextIndex]?.id ?? null
    }
  }

  /** 把 from 位置的组件移动到 to 位置（拖拽排序与上移/下移共用） */
  function moveComponent(from: number, to: number): void {
    if (!config.value) return
    const list = config.value.components
    if (from < 0 || from >= list.length || to < 0 || to >= list.length || from === to) return
    const [moved] = list.splice(from, 1)
    if (!moved) return
    list.splice(to, 0, moved)
  }

  function moveUp(id: string): void {
    if (!config.value) return
    const index = config.value.components.findIndex((item) => item.id === id)
    if (index > 0) moveComponent(index, index - 1)
  }

  function moveDown(id: string): void {
    if (!config.value) return
    const index = config.value.components.findIndex((item) => item.id === id)
    if (index >= 0 && index < config.value.components.length - 1) moveComponent(index, index + 1)
  }

  function toggleVisible(id: string): void {
    if (!config.value) return
    const component = findComponent(config.value, id)
    if (component) component.visible = !component.visible
  }

  function updateComponentTitle(id: string, title: string): void {
    if (!config.value) return
    const component = findComponent(config.value, id)
    if (component) component.title = title
  }

  function updateProp(id: string, key: string, value: ComponentPropValue): void {
    if (!config.value) return
    const component = findComponent(config.value, id)
    if (component) component.props[key] = value
  }

  /** 更新按钮动作（只重建协议允许的字段，切换类型时不残留旧字段） */
  function updateAction(id: string, action: PageAction): void {
    if (!config.value) return
    const component = findComponent(config.value, id)
    if (!component) return
    component.action = normalizeAction(action)
  }

  /* ---------------- 页面设置 ---------------- */
  function updatePageTitle(title: string): void {
    if (config.value) config.value.pageTitle = title
  }

  function updateTheme(patch: Partial<ThemeConfig>): void {
    if (!config.value) return
    config.value.theme = { ...config.value.theme, ...patch }
  }

  function applyThemePreset(theme: ThemeConfig): void {
    if (!config.value) return
    config.value.theme = { ...theme }
  }

  /** 保存前的前端轻校验上下文 */
  function validationContext() {
    return {
      projectSlug: project.value?.slug ?? '',
      pageId,
      targetPages: pages.value.map((item) => ({
        pageId: item.pageId,
        name: item.name,
        pageType: item.pageType,
        status: item.status,
        slug: item.slug,
      })),
    }
  }

  /* ---------------- 保存 ---------------- */
  async function save(): Promise<SaveResult> {
    if (!config.value || !project.value) return { ok: false }
    if (saving.value) return { ok: false }
    clearMessages()

    // 前端轻校验：errors 阻止保存；warnings（如目标页面未发布）允许保存但提示
    const validation = validateDraftConfig(config.value, validationContext())
    validationWarnings.value = [...validation.warnings]
    if (!validation.valid) {
      validationErrors.value = [...validation.errors]
      toast.error('页面配置校验未通过，请按提示修正后再保存')
      return { ok: false }
    }

    saving.value = true
    try {
      const envelope = await savePageDraft(
        projectId,
        pageId,
        cloneConfig(config.value),
        configVersion.value,
      )
      configVersion.value = envelope.configVersion
      config.value = cloneConfig(envelope.config)
      savedSnapshot.value = serializeConfig(config.value)
      lastSavedAt.value = envelope.updatedAt
      conflict.value = false
      saveError.value = ''
      fieldErrors.value = {}
      validationErrors.value = []
      if (page.value) {
        page.value = {
          ...page.value,
          draftVersion: envelope.configVersion,
          updatedAt: envelope.updatedAt,
        }
      }
      toast.success(`保存成功，当前草稿 v${envelope.configVersion}`)
      return { ok: true }
    } catch (error) {
      if (isApiError(error) && error.status === 409) {
        conflict.value = true
        saveError.value = '配置已被其他操作修改，请刷新后重试'
        toast.error('保存失败：配置已被其他操作修改，请刷新后重试')
        return { ok: false, conflict: true }
      }
      if (isApiError(error) && error.status === 400) {
        fieldErrors.value = { ...(error.fieldErrors ?? {}) }
        saveError.value = error.message || '页面配置校验失败，请检查后重试'
        toast.error(saveError.value)
        return { ok: false, fieldErrors: fieldErrors.value }
      }
      saveError.value = errorMessageOf(error, '保存失败，请稍后重试')
      toast.error(saveError.value)
      return { ok: false }
    } finally {
      saving.value = false
    }
  }

  /* ---------------- 发布（页面级） ---------------- */
  async function publish(): Promise<PublishPageResult | null> {
    if (publishing.value) return null
    publishing.value = true
    try {
      const result = await publishPage(projectId, pageId)
      if (page.value) {
        page.value = {
          ...page.value,
          status: 'PUBLISHED',
          publishedVersion: result.version,
        }
      }
      if (project.value) {
        project.value = {
          ...project.value,
          status: 'PUBLISHED',
          publishedVersion:
            page.value?.pageType === 'home' ? result.version : project.value.publishedVersion,
        }
      }
      await refreshPages()
      toast.success(`发布成功：v${result.version}，公开路径 ${result.publicPath}`)
      return result
    } catch (error) {
      if (isApiError(error) && error.status === 409) {
        toast.error(`发布失败：${error.message}（修改草稿并保存产生新版本后可再次发布）`)
      } else {
        toast.error(`发布失败：${errorMessageOf(error, '请稍后重试')}`)
      }
      return null
    } finally {
      publishing.value = false
    }
  }

  /* ---------------- 重置默认模板 ---------------- */
  async function resetDefault(): Promise<boolean> {
    if (resetting.value) return false
    resetting.value = true
    try {
      const envelope = await resetPageDraft(projectId, pageId)
      applyPageEnvelope(envelope)
      toast.success('已按页面模板重置草稿（不影响已发布快照）')
      return true
    } catch (error) {
      toast.error(`重置失败：${errorMessageOf(error, '请稍后重试')}`)
      return false
    } finally {
      resetting.value = false
    }
  }

  return {
    // 数据
    project,
    page,
    pages,
    settings,
    config,
    configVersion,
    lastSavedAt,
    selectedId,
    selectedComponent,
    selectedIndex,
    dirty,
    hasUnpublishedVersion,
    pageTypeLabel,
    pageStatusLabel,
    siteContext,
    loadWarnings,
    // 加载/保存状态
    loading,
    loadError,
    loadErrorStatus,
    saving,
    publishing,
    resetting,
    saveError,
    conflict,
    fieldErrors,
    validationErrors,
    validationWarnings,
    // 方法
    load,
    reloadFromServer,
    refreshPages,
    select,
    addComponent,
    removeComponent,
    moveComponent,
    moveUp,
    moveDown,
    toggleVisible,
    updateComponentTitle,
    updateProp,
    updateAction,
    updatePageTitle,
    updateTheme,
    applyThemePreset,
    save,
    publish,
    resetDefault,
    clearMessages,
  }
}

export type EditorState = ReturnType<typeof useEditorState>
