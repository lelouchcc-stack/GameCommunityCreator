<script setup lang="ts">
/**
 * Markdown 安全渲染器（v3）：渲染前经 marked + DOMPurify 严格清洗；
 * 输出仅用于受控 v-html（clean HTML），不执行任何脚本。
 */
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps<{
  markdown?: string | null
  /** 空内容提示 */
  emptyText?: string
}>()

const html = computed(() => renderMarkdown(props.markdown))
</script>

<template>
  <div v-if="html" class="markdown-body" v-html="html"></div>
  <p v-else class="markdown-renderer__empty gcc-muted">{{ emptyText ?? '暂无内容' }}</p>
</template>

<style scoped>
.markdown-renderer__empty {
  font-size: 13px;
}
</style>
