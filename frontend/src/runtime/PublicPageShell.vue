<script setup lang="ts">
/**
 * 公开站点页面外壳（所有公开端视图共用）：
 * 统一 loading 骨架 / 404 / 错误重试 / 布局 + 运行时渲染。
 *
 * 默认在布局内用 CommunityRuntime 渲染页面 JSON；
 * 需求要固定视图的页面（更新日志详情、文档详情）通过默认插槽覆盖内容区域。
 *
 * 页脚唯一（BUGFIX_PLAN §1）：
 * - 页面 JSON 渲染模式：配置里存在可见 footer 组件 → 关闭布局页脚（由组件渲染，且组件从布局
 *   inject 到页脚导航）；配置没有 footer 组件 → 布局页脚兜底；
 * - 固定详情页（默认插槽）→ 保留布局页脚（页面没有运行时 footer 组件，不会重复）。
 */
import { computed, useSlots } from 'vue'
import { RouterLink } from 'vue-router'
import PublicStateBlock from '@/components/public/PublicStateBlock.vue'
import type { PageConfig, ThemeConfig } from '@/types/page'
import type { PublicSiteDetail, SiteRuntimeContext } from '@/types/site'
import type { RuntimeDataLoader } from '@/types/runtime'
import CommunityRuntime from './CommunityRuntime.vue'
import PublicSiteLayout from './PublicSiteLayout.vue'
import { usePublicPageData } from './usePublicPageData'

const props = defineProps<{
  slug: string
  /** null = 首页接口（/community/:slug） */
  pageSlug?: string | null
  /** 自定义 404 文案 */
  notFoundText?: string
}>()

const slots = useSlots()

const slugRef = computed(() => props.slug ?? '')
const pageSlugRef = computed(() => props.pageSlug ?? null)

const { detail, loading, notFound, errorMessage, missingText, config, theme, loader, site, reload } =
  usePublicPageData({
    slug: slugRef,
    pageSlug: pageSlugRef,
    notFoundText: props.notFoundText,
  })

/** 固定详情页（使用默认插槽覆盖内容）：布局页脚负责页脚 */
const usesCustomContent = computed(() => Boolean(slots.default))

/** 页面配置里是否有可见 footer 组件（有则由它渲染页脚，布局不再渲染） */
const configHasVisibleFooter = computed(() => {
  const components = config.value?.components
  if (!Array.isArray(components)) return false
  return components.some((item) => item && item.type === 'footer' && item.visible !== false)
})

const showLayoutFooter = computed(
  () => usesCustomContent.value || !configHasVisibleFooter.value,
)

defineSlots<{
  default?(props: {
    detail: PublicSiteDetail
    config: PageConfig
    site: SiteRuntimeContext
    loader: RuntimeDataLoader
    theme: ThemeConfig | null
  }): unknown
}>()

defineExpose({ reload })
</script>

<template>
  <div class="public-shell">
    <div v-if="loading" class="public-shell__state gcc-container">
      <PublicStateBlock variant="loading" kicker="GAME PORTAL" title="正在加载游戏门户…" />
    </div>

    <div v-else-if="notFound" class="public-shell__state gcc-container">
      <PublicStateBlock
        variant="notfound"
        kicker="404 · NOT FOUND"
        :title="missingText"
        description="公开页面只展示管理员发布后的内容；如果站点刚创建或修改了草稿，需要先发布才会更新。"
      >
        <RouterLink class="gcc-btn gcc-btn-primary" to="/community">返回游戏门户广场</RouterLink>
        <RouterLink class="gcc-btn" to="/admin/projects">前往管理端</RouterLink>
      </PublicStateBlock>
    </div>

    <div v-else-if="errorMessage" class="public-shell__state gcc-container">
      <PublicStateBlock
        variant="error"
        kicker="ERROR · LOAD FAILED"
        title="页面加载失败"
        :description="errorMessage"
        @retry="reload"
      />
    </div>

    <PublicSiteLayout
      v-else-if="detail && config"
      :detail="detail"
      :page-theme="theme"
      :show-footer="showLayoutFooter"
    >
      <slot
        v-if="slots.default"
        :detail="detail"
        :config="config"
        :site="site"
        :loader="loader"
        :theme="theme"
      />
      <CommunityRuntime
        v-else
        :key="`public-${detail.page.pageId}`"
        :config="config"
        :loader="loader"
        :site="site"
        mode="public"
      />
    </PublicSiteLayout>
  </div>
</template>

<style scoped>
.public-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--gcc-bg);
  color: var(--gcc-text);
}

.public-shell__state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 96px 0;
}
</style>
