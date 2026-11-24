<script setup lang="ts">
/**
 * 公开友情链接（/community/:slug/links）。
 * 由页面 JSON 驱动（固定页面 slug = links），数据来自公开友链接口（仅启用项）。
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
  <PublicPageShell :slug="slug" page-slug="links" not-found-text="该页面不存在或尚未发布" />
</template>
