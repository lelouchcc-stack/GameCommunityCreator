<script setup lang="ts">
/**
 * 公开更新日志列表（/community/:slug/changelog）。
 *
 * 列表页同样由页面 JSON 驱动（编辑器可改标题/条数/版本徽标），
 * 页面 slug 固定为 changelog；详情页为固定视图。
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
</script>

<template>
  <PublicPageShell
    :slug="slug"
    page-slug="changelog"
    not-found-text="该页面不存在或尚未发布"
  />
</template>
