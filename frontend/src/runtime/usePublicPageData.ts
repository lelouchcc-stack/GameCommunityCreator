/**
 * 公开页面数据加载组合式函数（所有公开端视图共用）。
 *
 * - `/community/:slug`        → getPublicCommunity（首页）
 * - `/community/:slug/...`    → getPublicPage(slug, 固定页面 slug 或自定义页面 slug)
 *
 * 统一处理：loading / 404（未发布或不存在）/ 错误重试 / document.title。
 */
import { computed, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import { isApiError } from '@/api/client'
import { getPublicCommunity, getPublicPage } from '@/api/public'
import type { PageConfig } from '@/types/page'
import type {
  PublicSiteDetail,
  SiteRuntimeContext,
  ThemeMode,
} from '@/types/site'
import type { RuntimeDataLoader } from '@/types/runtime'
import { toErrorMessage } from './errors'
import { createPublicLoader } from './loaders'

export interface PublicPageDataOptions {
  slug: Ref<string>
  /** null = /community/:slug 首页接口；否则传页面 slug */
  pageSlug: Ref<string | null>
  /** 自定义 404 文案 */
  notFoundText?: string
}

const DEFAULT_HOME_MISSING = '该社区不存在或尚未发布'
const DEFAULT_PAGE_MISSING = '该页面不存在或尚未发布'

export function usePublicPageData(options: PublicPageDataOptions) {
  const detail = ref<PublicSiteDetail | null>(null)
  const loading = ref(true)
  const notFound = ref(false)
  const errorMessage = ref('')
  const missingText = ref(options.notFoundText ?? DEFAULT_PAGE_MISSING)

  let loadSeq = 0

  const config = computed<PageConfig | null>(() => detail.value?.page?.config ?? null)
  const loader = computed<RuntimeDataLoader>(() => createPublicLoader(options.slug.value))
  const theme = computed(() => config.value?.theme ?? null)

  const site = computed<SiteRuntimeContext>(() => {
    const current = detail.value
    if (!current) {
      return { projectId: 0, projectSlug: options.slug.value, pages: [], themeMode: 'DARK' }
    }
    return {
      projectId: current.project?.id ?? 0,
      projectSlug: current.project?.slug ?? options.slug.value,
      pages: Array.isArray(current.pages)
        ? current.pages.map((page) => ({
            pageId: page.pageId,
            name: page.name,
            slug: page.slug,
            pageType: page.pageType,
          }))
        : [],
      themeMode: current.settings?.themeMode ?? ('DARK' as ThemeMode),
    }
  })

  async function load(): Promise<void> {
    const seq = loadSeq + 1
    loadSeq = seq
    loading.value = true
    notFound.value = false
    errorMessage.value = ''

    const slug = options.slug.value.trim()
    const pageSlug = options.pageSlug.value
    missingText.value =
      options.notFoundText ??
      (pageSlug === null || pageSlug === '' ? DEFAULT_HOME_MISSING : DEFAULT_PAGE_MISSING)

    if (!slug) {
      detail.value = null
      notFound.value = true
      loading.value = false
      document.title = `${missingText.value} · GameCommunityCreator`
      return
    }

    try {
      const data =
        pageSlug === null || pageSlug === ''
          ? await getPublicCommunity(slug)
          : await getPublicPage(slug, pageSlug)
      if (seq !== loadSeq) return
      detail.value = data
      const pageTitle = (data.page?.config?.pageTitle || data.page?.name || '').trim()
      const gameName = (data.project?.gameName || data.project?.name || '').trim()
      document.title = pageTitle
        ? `${pageTitle}${gameName ? ` · ${gameName}` : ''}`
        : gameName || 'GameCommunityCreator'
    } catch (error) {
      if (seq !== loadSeq) return
      detail.value = null
      if (isApiError(error) && error.status === 404) {
        notFound.value = true
        document.title = `${missingText.value} · GameCommunityCreator`
      } else {
        errorMessage.value = toErrorMessage(error, '页面加载失败，请稍后重试')
      }
    } finally {
      if (seq === loadSeq) loading.value = false
    }
  }

  watch([options.slug, options.pageSlug], () => void load(), { immediate: true })

  onBeforeUnmount(() => {
    loadSeq += 1
  })

  return {
    detail,
    loading,
    notFound,
    errorMessage,
    missingText,
    config,
    theme,
    loader,
    site,
    reload: load,
  }
}
