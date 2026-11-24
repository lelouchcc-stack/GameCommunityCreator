<script setup lang="ts">
/**
 * 公开站点首页（/community/:slug）：只读已发布快照。
 *
 * 配置来自 GET /api/public/communities/{slug} → page.config（发布快照，不含草稿改动），
 * 数据走 createPublicLoader(slug) 的公开只读接口。
 * 未发布 / 不存在统一 404，展示友好提示，不泄漏任何草稿内容。
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
  <PublicPageShell :slug="slug" :page-slug="null" />
</template>
