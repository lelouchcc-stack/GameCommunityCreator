<script setup lang="ts">
/**
 * 更新日志详情（/community/:slug/changelog/:entryId）—— 固定视图，不进入页面管理。
 *
 * 数据：GET /api/public/communities/{slug}/changelogs/{entryId}（非 PUBLISHED → 404）。
 * 正文用共享 MarkdownRenderer 渲染 `bodyMarkdown || content`（marked + DOMPurify 清洗后的受控 HTML），
 * 不直接输出原文、不执行任何脚本。
 *
 * 视觉：软件 Release Notes 排版 —— mono 大号版本号 + 发布日期 + display 标题 + Markdown 正文，
 * 颜色全部来自门户主题 --gcc-* 变量。
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { isApiError } from '@/api/client'
import { getPublicChangelog } from '@/api/public'
import MarkdownRenderer from '@/components/markdown/MarkdownRenderer.vue'
import PublicStateBlock from '@/components/public/PublicStateBlock.vue'
import PublicPageShell from '@/runtime/PublicPageShell.vue'
import { toErrorMessage } from '@/runtime/errors'
import type { ChangelogView } from '@/types/content'
import { formatDate, formatDateTime } from '@/utils/format'

const route = useRoute()

const slug = computed(() => {
  const raw = route.params.slug
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' ? value : ''
})

const entryId = computed(() => {
  const raw = route.params.entryId
  const value = Array.isArray(raw) ? raw[0] : raw
  const numeric = Number(value)
  return Number.isInteger(numeric) && numeric > 0 ? numeric : 0
})

const entry = ref<ChangelogView | null>(null)
const loading = ref(true)
const notFound = ref(false)
const errorMessage = ref('')

let loadSeq = 0

async function loadEntry(): Promise<void> {
  const seq = loadSeq + 1
  loadSeq = seq
  loading.value = true
  notFound.value = false
  errorMessage.value = ''
  entry.value = null

  if (!slug.value || !entryId.value) {
    notFound.value = true
    loading.value = false
    return
  }

  try {
    const data = await getPublicChangelog(slug.value, entryId.value)
    if (seq !== loadSeq) return
    entry.value = data
  } catch (error) {
    if (seq !== loadSeq) return
    if (isApiError(error) && error.status === 404) {
      notFound.value = true
    } else {
      errorMessage.value = toErrorMessage(error, '更新日志加载失败，请稍后重试')
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

watch([slug, entryId], () => void loadEntry(), { immediate: true })

onBeforeUnmount(() => {
  loadSeq += 1
})
</script>

<template>
  <PublicPageShell :slug="slug" page-slug="changelog">
    <template #default="{ detail }">
      <div class="release gcc-container">
        <div class="release__topbar">
          <RouterLink
            class="release__back"
            :to="`/community/${detail.project.slug}/changelog`"
          >
            ← 返回更新日志
          </RouterLink>
          <span class="release__kicker">RELEASE NOTES</span>
        </div>

        <PublicStateBlock
          v-if="loading"
          variant="loading"
          kicker="RELEASE NOTES"
          title="正在加载更新日志…"
        />

        <PublicStateBlock
          v-else-if="notFound"
          variant="notfound"
          kicker="404 · RELEASE NOTES"
          title="该更新日志不存在或尚未发布"
          description="它可能已被删除，或者管理员还没有发布这一条更新。"
        >
          <RouterLink
            class="gcc-btn gcc-btn-primary"
            :to="`/community/${detail.project.slug}/changelog`"
          >
            查看全部更新日志
          </RouterLink>
        </PublicStateBlock>

        <PublicStateBlock
          v-else-if="errorMessage"
          variant="error"
          kicker="ERROR · RELEASE NOTES"
          title="更新日志加载失败"
          :description="errorMessage"
          @retry="loadEntry"
        />

        <article v-else-if="entry" class="release-article">
          <header class="release-article__head">
            <div class="release-article__labels">
              <span class="release-article__kicker">RELEASE NOTES</span>
              <span v-if="entry.version" class="release-article__version">{{ entry.version }}</span>
            </div>
            <h1 class="release-article__title">{{ entry.title }}</h1>
            <p class="release-article__meta">
              <span>RELEASED {{ formatDate(entry.releaseDate) }}</span>
              <span class="release-article__dot" aria-hidden="true">·</span>
              <span>UPDATED {{ formatDateTime(entry.updatedAt) }}</span>
            </p>
          </header>

          <div class="release-article__body">
            <MarkdownRenderer :markdown="entry.bodyMarkdown || entry.content" empty-text="暂无更新说明" />
          </div>
        </article>
      </div>
    </template>
  </PublicPageShell>
</template>

<style scoped>
.release {
  padding: 36px 0 96px;
}

.release__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 22px;
  margin-bottom: 40px;
  border-bottom: 1px solid var(--gcc-border);
}

.release__back {
  font-family: var(--gcc-mono);
  font-size: 10.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  transition: color 0.15s ease;
}

.release__back:hover {
  color: var(--gcc-text);
}

.release__kicker {
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.18em;
  color: var(--gcc-muted);
  opacity: 0.8;
}

.release-article {
  max-width: 920px;
}

.release-article__head {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 30px;
  border-bottom: 1px solid var(--gcc-border);
}

.release-article__labels {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.release-article__kicker {
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

.release-article__version {
  font-family: var(--gcc-mono);
  font-size: clamp(34px, 4.4vw, 58px);
  font-weight: 400;
  line-height: 1.02;
  letter-spacing: -0.04em;
  color: var(--gcc-text);
}

.release-article__title {
  color: var(--gcc-text);
  font-size: clamp(24px, 2.8vw, 38px);
  font-weight: 400;
  line-height: 1.12;
  letter-spacing: -0.02em;
}

.release-article__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

.release-article__dot {
  opacity: 0.6;
}

.release-article__body {
  padding-top: 30px;
  max-width: 860px;
  color: var(--gcc-text);
  font-size: 15px;
  word-break: break-word;
}

@media (max-width: 720px) {
  .release {
    padding: 24px 0 64px;
  }

  .release__topbar {
    margin-bottom: 24px;
  }

  .release-article__title {
    font-size: 22px;
  }
}
</style>
