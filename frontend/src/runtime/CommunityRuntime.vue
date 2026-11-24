<script setup lang="ts">
/**
 * CommunityRuntime — JSON 配置驱动的社区页面运行时（契约：AGENT_CONTRACT §3.6）。
 *
 * 职责边界：
 * - 只负责「按配置渲染 + 按数据源取数 + 四态分发」，不含任何业务写死数据。
 * - 编辑器画布、草稿预览、公开页共用本组件，差异只由 loader + mode + site 决定。
 *
 * 加载规则（PAGE_SCHEMA §5）：
 * 1. 按 components[].dataSource 去重：同一数据源一次加载只请求一次；
 * 2. 同源多组件取最大 limit；docs 额外按 category 分组去重（不同分类各自请求一次）；
 * 3. visible === false 的组件不渲染、也不触发其数据源请求；
 * 4. watch 数据源签名（source + limit + category）才重取，改标题/主题/文案不重取；
 * 5. 每个数据源独立 loading / ready / empty / error 四态，错误可单独重试；
 * 6. 请求带序号，过期响应（组件已隐藏 / 已有新请求）直接丢弃，避免竞态覆盖。
 *
 * 主题：页面 theme 提供品牌色，站点 theme_mode + 访客偏好决定明暗；
 * PublicSiteLayout 通过 inject 下发最终模式，无布局时运行时自行解析兜底。
 */
import {
  computed,
  inject,
  onBeforeUnmount,
  onMounted,
  provide,
  reactive,
  watch,
  type Component as VueComponent,
  type ComputedRef,
} from 'vue'
import { getComponentDef, isComponentType } from '@/components/registry'
import {
  DEFAULT_THEME,
  type PageComponent,
  type PageConfig,
} from '@/types/page'
import type { ComponentStatus, RuntimeData, RuntimeDataLoader } from '@/types/runtime'
import type { SiteRuntimeContext } from '@/types/site'
import { readStoredThemeMode, resolveThemeMode } from '@/utils/theme'
import { getRuntimeComponent } from './componentMap'
import { asGameInfo, readBoundedNumber, readString } from './componentProps'
import { RUNTIME_MODE_KEY } from './context'
import { toErrorMessage } from './errors'
import {
  RUNTIME_THEME_KEY,
  buildThemeStyle,
  ensureSystemThemeListener,
  systemPrefersDark,
  type EffectiveTheme,
} from './themeContext'
import type { RuntimeMode } from './types'
import RuntimeToast from './components/RuntimeToast.vue'

const props = withDefaults(
  defineProps<{
    config: PageConfig
    loader: RuntimeDataLoader
    mode?: RuntimeMode
    /** 站点运行时上下文；编辑器画布可省略（动作解析会提示上下文未就绪） */
    site?: SiteRuntimeContext
  }>(),
  {
    mode: 'public',
    site: () => ({ projectId: 0, projectSlug: '', pages: [], themeMode: 'DARK' }),
  },
)

/* ------------------------------ 主题 / 模式 ------------------------------ */

const injectedTheme = inject(RUNTIME_THEME_KEY, null) as ComputedRef<EffectiveTheme> | null

const fallbackTheme = computed<EffectiveTheme>(() => {
  const stored = props.mode === 'public' ? readStoredThemeMode(props.site.projectSlug) : null
  return resolveThemeMode(props.site.themeMode, stored, systemPrefersDark.value)
})

const effectiveTheme = computed<EffectiveTheme>(
  () => injectedTheme?.value ?? fallbackTheme.value,
)

const theme = computed(() => props.config.theme ?? DEFAULT_THEME)

const rootStyle = computed<Record<string, string>>(() =>
  buildThemeStyle(theme.value, effectiveTheme.value),
)

provide(RUNTIME_MODE_KEY, computed(() => props.mode))

onMounted(() => ensureSystemThemeListener())

/* ------------------------------ 数据源规划 ------------------------------ */

type ListSourceId =
  | 'game_info'
  | 'announcements'
  | 'changelogs'
  | 'team'
  | 'links'
  | 'issues_progress'

const LIST_SOURCES: readonly ListSourceId[] = [
  'game_info',
  'announcements',
  'changelogs',
  'team',
  'links',
  'issues_progress',
]

const MAX_LIMIT_BY_SOURCE: Record<ListSourceId, number> = {
  game_info: 1,
  announcements: 10,
  changelogs: 20,
  team: 1,
  links: 1,
  issues_progress: 20,
}

interface SourceState {
  status: ComponentStatus
  data: RuntimeData
  errorMessage: string
}

function createState(): SourceState {
  return { status: 'loading', data: null, errorMessage: '' }
}

const listStates = reactive<Record<ListSourceId, SourceState>>({
  game_info: createState(),
  announcements: createState(),
  changelogs: createState(),
  team: createState(),
  links: createState(),
  issues_progress: createState(),
})

/** docs 按 category 分组（空字符串 = 全部分类） */
const docStates = reactive<Record<string, SourceState>>({})

const listLimits = reactive<Record<ListSourceId, number>>({
  game_info: 0,
  announcements: 0,
  changelogs: 0,
  team: 0,
  links: 0,
  issues_progress: 0,
})

const docLimits = reactive<Record<string, number>>({})

const listSeq: Record<ListSourceId, number> = {
  game_info: 0,
  announcements: 0,
  changelogs: 0,
  team: 0,
  links: 0,
  issues_progress: 0,
}

const docSeq: Record<string, number> = {}

/** 类型默认 limit（未注册类型回退 6） */
function defaultLimitFor(type: string): number {
  if (!isComponentType(type)) return 6
  const fallback = getComponentDef(type).defaultProps.limit
  return typeof fallback === 'number' ? fallback : 6
}

/** 组件声明的列表条数：夹到该数据源的契约上限，坏值回退注册表默认值 */
function normalizeLimit(component: PageComponent): number {
  const fallback = defaultLimitFor(component.type)
  const max = component.dataSource
    ? MAX_LIMIT_BY_SOURCE[component.dataSource as ListSourceId] ?? 10
    : 10
  return readBoundedNumber(component, 'limit', fallback, 1, Math.max(1, max))
}

/** docs 组件的分类筛选值（去空白；空 = 全部） */
function docCategory(component: PageComponent): string {
  return readString(component, 'category').trim()
}

interface SourcePlan {
  lists: Record<ListSourceId, number>
  docs: Record<string, number>
}

/** 由当前配置算出「每个数据源需要请求什么」 */
function computePlan(config: PageConfig): SourcePlan {
  const lists: Record<ListSourceId, number> = {
    game_info: 0,
    announcements: 0,
    changelogs: 0,
    team: 0,
    links: 0,
    issues_progress: 0,
  }
  const docs: Record<string, number> = {}
  const components = Array.isArray(config.components) ? config.components : []
  for (const component of components) {
    if (!component || component.visible === false) continue
    const source = component.dataSource
    if (!source) continue
    if (source === 'docs') {
      const category = docCategory(component)
      docs[category] = Math.max(docs[category] ?? 0, normalizeLimit(component))
      continue
    }
    if (source === 'game_info' || source === 'team' || source === 'links') {
      lists[source] = 1
      continue
    }
    lists[source] = Math.max(lists[source], normalizeLimit(component))
  }
  return { lists, docs }
}

/* ------------------------------ 数据加载 ------------------------------ */

function normalizeStatus(source: ListSourceId, data: RuntimeData): ComponentStatus {
  if (source === 'game_info') {
    const info = asGameInfo(data)
    return info && info.name.trim() ? 'ready' : 'empty'
  }
  return Array.isArray(data) && data.length > 0 ? 'ready' : 'empty'
}

async function fetchList(source: ListSourceId, limit: number): Promise<RuntimeData> {
  switch (source) {
    case 'game_info':
      return props.loader.loadGameInfo()
    case 'announcements':
      return props.loader.loadAnnouncements(limit)
    case 'changelogs':
      return props.loader.loadChangelogs(limit)
    case 'team':
      return props.loader.loadTeam()
    case 'links':
      return props.loader.loadLinks()
    case 'issues_progress':
      return props.loader.loadIssueProgress(limit)
    default:
      return null
  }
}

async function loadList(source: ListSourceId, limit: number): Promise<void> {
  const seq = listSeq[source] + 1
  listSeq[source] = seq
  const state = listStates[source]
  state.status = 'loading'
  state.data = null
  state.errorMessage = ''
  try {
    const data = await fetchList(source, limit)
    if (seq !== listSeq[source]) return
    state.data = data ?? null
    state.status = normalizeStatus(source, state.data)
  } catch (error) {
    if (seq !== listSeq[source]) return
    state.data = null
    state.status = 'error'
    state.errorMessage = toErrorMessage(error)
  }
}

async function loadDocs(category: string, limit: number): Promise<void> {
  const seq = (docSeq[category] ?? 0) + 1
  docSeq[category] = seq
  if (!docStates[category]) docStates[category] = createState()
  const state = docStates[category]
  state.status = 'loading'
  state.data = null
  state.errorMessage = ''
  try {
    const data = await props.loader.loadDocs(limit, category)
    if (seq !== (docSeq[category] ?? 0)) return
    state.data = Array.isArray(data) ? data : []
    state.status = (state.data as unknown[]).length > 0 ? 'ready' : 'empty'
  } catch (error) {
    if (seq !== (docSeq[category] ?? 0)) return
    state.data = null
    state.status = 'error'
    state.errorMessage = toErrorMessage(error)
  }
}

/**
 * 数据源签名：数据源 + 实际请求参数（limit / category）。
 * watch 它而不是 watch 整个 config —— 改标题/主题/文案签名不变，不会重新请求。
 */
const sourceSignature = computed(() => {
  const plan = computePlan(props.config)
  const lists = LIST_SOURCES.map((source) => `${source}:${plan.lists[source]}`).join(',')
  const docs = Object.keys(plan.docs)
    .sort()
    .map((category) => `docs:${category}:${plan.docs[category]}`)
    .join(',')
  return `${lists}|${docs}`
})

/** 让内部状态与最新配置对齐：仅在参数变化（或 force）时发请求 */
function syncSources(force = false): void {
  const plan = computePlan(props.config)

  for (const source of LIST_SOURCES) {
    const limit = plan.lists[source]
    const previousLimit = listLimits[source]
    listLimits[source] = limit
    if (limit <= 0) {
      if (previousLimit > 0) {
        // 该数据源已无可见组件：作废旧请求并清空状态
        listSeq[source] += 1
        const state = listStates[source]
        state.status = 'loading'
        state.data = null
        state.errorMessage = ''
      }
      continue
    }
    if (force || previousLimit !== limit) {
      void loadList(source, limit)
    }
  }

  // docs：删除已无组件的分类（并作废其进行中请求）
  for (const category of Object.keys(docStates)) {
    if (!(category in plan.docs)) {
      docSeq[category] = (docSeq[category] ?? 0) + 1
      delete docStates[category]
    }
  }
  for (const category of Object.keys(docLimits)) {
    if (!(category in plan.docs)) delete docLimits[category]
  }
  for (const category of Object.keys(plan.docs)) {
    const limit = plan.docs[category]
    const previousLimit = docLimits[category] ?? 0
    docLimits[category] = limit
    if (force || previousLimit !== limit || !docStates[category]) {
      void loadDocs(category, limit)
    }
  }
}

function retryComponentSource(component: PageComponent): void {
  const source = component.dataSource
  if (!source) return
  if (source === 'docs') {
    const category = docCategory(component)
    const limit = docLimits[category] ?? normalizeLimit(component)
    void loadDocs(category, limit)
    return
  }
  const listSource = source as ListSourceId
  const limit = listLimits[listSource] || normalizeLimit(component)
  void loadList(listSource, limit)
}

watch(sourceSignature, () => syncSources(false), { immediate: true })
watch(
  () => props.loader,
  () => syncSources(true),
)

onBeforeUnmount(() => {
  for (const source of LIST_SOURCES) listSeq[source] += 1
  for (const category of Object.keys(docSeq)) docSeq[category] += 1
})

/* ------------------------------ 渲染行 ------------------------------ */

const NOOP = (): void => {}

interface RenderRow {
  key: string
  component: PageComponent
  runtimeComponent: VueComponent | null
  status: ComponentStatus
  data: RuntimeData
  errorMessage: string
  retry: () => void
}

/** 组件当前数据源状态（未知数据源回退 ready —— 不阻塞渲染） */
function stateFor(component: PageComponent): SourceState | null {
  const source = component.dataSource
  if (!source) return null
  if (source === 'docs') return docStates[docCategory(component)] ?? null
  return listStates[source as ListSourceId] ?? null
}

/** 按 config.components 数组顺序生成可见组件行（隐藏组件在此被过滤） */
const renderRows = computed<RenderRow[]>(() => {
  const components = Array.isArray(props.config.components) ? props.config.components : []
  const rows: RenderRow[] = []
  components.forEach((component, index) => {
    if (!component || component.visible === false) return
    const state = stateFor(component)
    rows.push({
      key: component.id || `${component.type}-${index}`,
      component,
      runtimeComponent: getRuntimeComponent(component.type),
      status: state ? state.status : 'ready',
      data: state ? state.data : null,
      errorMessage: state ? state.errorMessage : '',
      retry: component.dataSource ? () => retryComponentSource(component) : NOOP,
    })
  })
  return rows
})

/** 供父组件强制重取全部数据（如预览页重新发布后手动刷新） */
function refreshAll(): void {
  syncSources(true)
}

defineExpose({ refreshAll })
</script>

<template>
  <div
    class="gcc-runtime"
    :class="`gcc-runtime--${mode}`"
    :style="rootStyle"
    :data-mode="mode"
    :data-theme="effectiveTheme"
    data-testid="community-runtime"
  >
    <div
      v-for="row in renderRows"
      :key="row.key"
      class="gcc-runtime__block"
      :data-component-id="row.component.id"
      :data-component-type="row.component.type"
      :data-status="row.status"
    >
      <component
        :is="row.runtimeComponent"
        v-if="row.runtimeComponent"
        :component="row.component"
        :status="row.status"
        :data="row.data"
        :error-message="row.errorMessage"
        :retry="row.retry"
        :site="site"
      />
      <div v-else class="gcc-runtime__unknown gcc-container">
        <div class="gcc-empty">不支持的组件类型：{{ row.component.type }}</div>
      </div>
    </div>

    <RuntimeToast />
  </div>
</template>

<style scoped>
.gcc-runtime {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 100%;
  font-family: var(--gcc-font);
  isolation: isolate;
}

.gcc-runtime__block {
  width: 100%;
}

.gcc-runtime__unknown {
  padding: 24px 0;
}

.gcc-runtime--editor {
  min-height: auto;
}
</style>

<!--
  门户主题下的通用类适配（非 scoped，但选择器全部限定在 .gcc-runtime 内）：
  全局 .gcc-btn / .gcc-input / .gcc-badge 等使用管理端 --x-* 令牌，
  在公开端/预览的站点门户主题里需要映射回 --gcc-*（含浅色门户）。
-->
<style>
.gcc-runtime .gcc-btn {
  border-color: var(--gcc-border-strong);
  background: transparent;
  color: var(--gcc-text);
  border-radius: 9999px;
}

.gcc-runtime .gcc-btn:hover:not(:disabled) {
  border-color: var(--gcc-text);
  background: var(--gcc-hover);
  color: var(--gcc-text);
}

.gcc-runtime .gcc-btn-primary {
  border-color: var(--gcc-text);
  background: var(--gcc-text);
  color: var(--gcc-bg);
}

.gcc-runtime .gcc-btn-primary:hover:not(:disabled) {
  border-color: var(--gcc-text);
  background: var(--gcc-text);
  color: var(--gcc-bg);
  opacity: 0.9;
}

.gcc-runtime .gcc-btn-ghost {
  border-color: transparent;
  color: var(--gcc-muted);
}

.gcc-runtime .gcc-btn-ghost:hover:not(:disabled) {
  color: var(--gcc-text);
  background: var(--gcc-hover);
}

.gcc-runtime .gcc-badge {
  border-color: var(--gcc-border-strong);
  background: var(--gcc-badge-bg);
  color: var(--gcc-muted);
}

.gcc-runtime .gcc-label {
  color: var(--gcc-muted);
}

.gcc-runtime .gcc-input,
.gcc-runtime .gcc-textarea,
.gcc-runtime .gcc-select {
  background: var(--gcc-input-bg);
  border-color: var(--gcc-border);
  color: var(--gcc-text);
  border-radius: 8px;
}

.gcc-runtime .gcc-input::placeholder,
.gcc-runtime .gcc-textarea::placeholder {
  color: var(--gcc-muted);
  opacity: 0.7;
}

.gcc-runtime .gcc-input:focus,
.gcc-runtime .gcc-textarea:focus,
.gcc-runtime .gcc-select:focus {
  border-color: var(--gcc-border-strong);
}

.gcc-runtime .gcc-card {
  border-color: var(--gcc-border);
  border-radius: var(--gcc-card-radius, 8px);
  background: var(--gcc-card-bg);
  box-shadow: none;
}

.gcc-runtime .gcc-empty,
.gcc-runtime .gcc-loading,
.gcc-runtime .gcc-error {
  border: 1px solid var(--gcc-border);
  border-radius: var(--gcc-card-radius, 8px);
  background: transparent;
  color: var(--gcc-muted);
}

.gcc-runtime .gcc-skeleton {
  background: var(--gcc-hover);
}

.gcc-runtime .gcc-mono {
  font-family: var(--gcc-mono);
}

.gcc-runtime a {
  color: var(--gcc-text-secondary, var(--gcc-text));
}

.gcc-runtime a:hover {
  color: var(--gcc-text);
}
</style>

