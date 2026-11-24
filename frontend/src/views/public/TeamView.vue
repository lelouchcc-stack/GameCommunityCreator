<script setup lang="ts">
/**
 * 公开制作团队（/community/:slug/team）。
 * 由页面 JSON 驱动（固定页面 slug = team），数据来自团队接口。
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
  <PublicPageShell :slug="slug" page-slug="team" not-found-text="该页面不存在或尚未发布" />
</template>
