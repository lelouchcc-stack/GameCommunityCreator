<script setup lang="ts">
/**
 * 公开文档列表（/community/:slug/docs）。
 * 列表由页面 JSON 驱动（固定页面 slug = docs），详情页为固定视图。
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
  <PublicPageShell :slug="slug" page-slug="docs" not-found-text="该页面不存在或尚未发布" />
</template>
