<script setup lang="ts">
/**
 * 公开自定义页面（/community/:slug/p/:pageSlug）。
 *
 * 由页面 JSON 驱动渲染（getPublicPage），与首页共用布局与运行时；
 * 页面未发布 / 不存在 → "该页面不存在或尚未发布"。
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PublicPageShell from '@/runtime/PublicPageShell.vue'

const route = useRoute()

const slug = computed(() => {
  const raw = route.params.slug
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' ? value : ''
})

const pageSlug = computed(() => {
  const raw = route.params.pageSlug
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' ? value : ''
})
</script>

<template>
  <PublicPageShell
    :slug="slug"
    :page-slug="pageSlug"
    not-found-text="该页面不存在或尚未发布"
  />
</template>
