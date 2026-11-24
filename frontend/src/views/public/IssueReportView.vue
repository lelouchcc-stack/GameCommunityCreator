<script setup lang="ts">
/**
 * 公开问题汇报（/community/:slug/report）。
 *
 * 由页面 JSON 驱动（固定页面 slug = report），页面内 issue_form 组件在公开模式下
 * 真正提交 POST /api/public/communities/{slug}/issues。
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
  <PublicPageShell :slug="slug" page-slug="report" not-found-text="该页面不存在或尚未发布" />
</template>
