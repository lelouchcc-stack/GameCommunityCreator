<script setup lang="ts">
/**
 * 公开文档详情（/community/:slug/docs/:docSlug）—— 固定视图，不进入页面管理。
 *
 * 数据：GET /api/public/communities/{slug}/docs/{docSlug}（非 PUBLISHED → 404）。
 * 正文用共享 MarkdownRenderer 渲染 `bodyMarkdown || content`（marked + DOMPurify 清洗后的受控 HTML），
 * 不直接输出原文、不执行任何脚本。
 *
 * 视觉：xAI 文档页 —— mono 分类标签 + display 标题 + 更新时间 + Markdown 正文（.markdown-body）。
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { isApiError } from '@/api/client'
import { getPublicDoc } from '@/api/public'
import MarkdownRenderer from '@/components/markdown/MarkdownRenderer.vue'
import PublicStateBlock from '@/components/public/PublicStateBlock.vue'
import PublicPageShell from '@/runtime/PublicPageShell.vue'
import { toErrorMessage } from '@/runtime/errors'
import type { DocView } from '@/types/content'
import { formatDateTime } from '@/utils/format'

const route = useRoute()

const slug = computed(() => {
  const raw = route.params.slug
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' ? value : ''
})

const docSlug = computed(() => {
  const raw = route.params.docSlug
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' ? value : ''
})

const doc = ref<DocView | null>(null)
const loading = ref(true)
const notFound = ref(false)
const errorMessage = ref('')

let loadSeq = 0

async function loadDoc(): Promise<void> {
  const seq = loadSeq + 1
  loadSeq = seq
  loading.value = true
  notFound.value = false
  errorMessage.value = ''
  doc.value = null

  if (!slug.value || !docSlug.value) {
    notFound.value = true
    loading.value = false
    return
  }

  try {
    const data = await getPublicDoc(slug.value, docSlug.value)
    if (seq !== loadSeq) return
    doc.value = data
  } catch (error) {
    if (seq !== loadSeq) return
    if (isApiError(error) && error.status === 404) {
      notFound.value = true
    } else {
      errorMessage.value = toErrorMessage(error, '文档加载失败，请稍后重试')
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

watch([slug, docSlug], () => void loadDoc(), { immediate: true })

onBeforeUnmount(() => {
  loadSeq += 1
})
</script>

<template>
  <PublicPageShell :slug="slug" page-slug="docs">
    <template #default="{ detail }">
      <div class="doc-detail gcc-container">
        <div class="doc-detail__topbar">
          <RouterLink class="doc-detail__back" :to="`/community/${detail.project.slug}/docs`">
            ← 返回公开文档
          </RouterLink>
          <span class="doc-detail__kicker">DOCUMENTS</span>
        </div>

        <PublicStateBlock
          v-if="loading"
          variant="loading"
          kicker="DOCUMENTS"
          title="正在加载文档…"
        />

        <PublicStateBlock
          v-else-if="notFound"
          variant="notfound"
          kicker="404 · DOCUMENTS"
          title="该文档不存在或尚未发布"
          description="它可能已被删除，或者管理员还没有发布这篇文档。"
        >
          <RouterLink class="gcc-btn gcc-btn-primary" :to="`/community/${detail.project.slug}/docs`">
            查看全部公开文档
          </RouterLink>
        </PublicStateBlock>

        <PublicStateBlock
          v-else-if="errorMessage"
          variant="error"
          kicker="ERROR · DOCUMENTS"
          title="文档加载失败"
          :description="errorMessage"
          @retry="loadDoc"
        />

        <article v-else-if="doc" class="doc-article">
          <header class="doc-article__head">
            <div class="doc-article__labels">
              <span class="doc-article__category">{{ doc.category || '未分类' }}</span>
              <span class="doc-article__meta">UPDATED {{ formatDateTime(doc.updatedAt) }}</span>
            </div>
            <h1 class="doc-article__title">{{ doc.title }}</h1>
          </header>

          <div class="doc-article__body">
            <MarkdownRenderer :markdown="doc.bodyMarkdown || doc.content" empty-text="暂无文档正文" />
          </div>
        </article>
      </div>
    </template>
  </PublicPageShell>
</template>

<style scoped>
.doc-detail {
  padding: 36px 0 96px;
}

.doc-detail__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 22px;
  margin-bottom: 40px;
  border-bottom: 1px solid var(--gcc-border);
}

.doc-detail__back {
  font-family: var(--gcc-mono);
  font-size: 10.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  transition: color 0.15s ease;
}

.doc-detail__back:hover {
  color: var(--gcc-text);
}

.doc-detail__kicker {
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.18em;
  color: var(--gcc-muted);
  opacity: 0.8;
}

.doc-article {
  max-width: 920px;
}

.doc-article__head {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 30px;
  border-bottom: 1px solid var(--gcc-border);
}

.doc-article__labels {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.doc-article__category {
  padding: 3px 10px;
  border-radius: 9999px;
  border: 1px solid var(--gcc-border-strong);
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-text-secondary, var(--gcc-text));
}

.doc-article__meta {
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

.doc-article__title {
  color: var(--gcc-text);
  font-size: clamp(28px, 3.2vw, 44px);
  font-weight: 400;
  line-height: 1.08;
  letter-spacing: -0.025em;
}

.doc-article__body {
  padding-top: 30px;
  max-width: 860px;
  color: var(--gcc-text);
  font-size: 15px;
  word-break: break-word;
}

@media (max-width: 720px) {
  .doc-detail {
    padding: 24px 0 64px;
  }

  .doc-detail__topbar {
    margin-bottom: 24px;
  }

  .doc-article__title {
    font-size: 24px;
  }
}
</style>
