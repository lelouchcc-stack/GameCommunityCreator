<script setup lang="ts">
/**
 * 公告详情（/community/:slug/news/:announcementId，路由 page-news-detail）—— 固定视图。
 *
 * 数据：GET /api/public/communities/{slug}/news/{announcementId}（公开只读）。
 * 正文用共享 MarkdownRenderer 渲染 `bodyMarkdown || content`（marked + DOMPurify 清洗后的受控 HTML），
 * 页脚仍由 PublicPageShell 的布局提供（唯一）。
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { isApiError } from '@/api/client'
import { getPublicAnnouncement } from '@/api/public'
import MarkdownRenderer from '@/components/markdown/MarkdownRenderer.vue'
import PublicStateBlock from '@/components/public/PublicStateBlock.vue'
import PublicPageShell from '@/runtime/PublicPageShell.vue'
import { toErrorMessage } from '@/runtime/errors'
import type { Announcement } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const route = useRoute()

const slug = computed(() => {
  const raw = route.params.slug
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' ? value : ''
})

const announcementId = computed(() => {
  const raw = route.params.announcementId
  const value = Array.isArray(raw) ? raw[0] : raw
  const numeric = Number(value)
  return Number.isInteger(numeric) && numeric > 0 ? numeric : 0
})

const announcement = ref<Announcement | null>(null)
const loading = ref(true)
const notFound = ref(false)
const errorMessage = ref('')

let loadSeq = 0

async function loadAnnouncement(): Promise<void> {
  const seq = loadSeq + 1
  loadSeq = seq
  loading.value = true
  notFound.value = false
  errorMessage.value = ''
  announcement.value = null

  if (!slug.value || !announcementId.value) {
    notFound.value = true
    loading.value = false
    return
  }

  try {
    const data = await getPublicAnnouncement(slug.value, announcementId.value)
    if (seq !== loadSeq) return
    announcement.value = data
  } catch (error) {
    if (seq !== loadSeq) return
    if (isApiError(error) && error.status === 404) {
      notFound.value = true
    } else {
      errorMessage.value = toErrorMessage(error, '公告加载失败，请稍后重试')
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

watch([slug, announcementId], () => void loadAnnouncement(), { immediate: true })

onBeforeUnmount(() => {
  loadSeq += 1
})
</script>

<template>
  <PublicPageShell :slug="slug" page-slug="home" not-found-text="该页面不存在或尚未发布">
    <template #default="{ detail }">
      <div class="news-detail gcc-container">
        <div class="news-detail__topbar">
          <RouterLink class="news-detail__back" :to="`/community/${detail.project.slug}`">
            ← 返回首页
          </RouterLink>
          <span class="news-detail__kicker">NEWS / ANNOUNCEMENT</span>
        </div>

        <PublicStateBlock
          v-if="loading"
          variant="loading"
          kicker="NEWS"
          title="正在加载公告…"
        />

        <PublicStateBlock
          v-else-if="notFound"
          variant="notfound"
          kicker="404 · NEWS"
          title="该公告不存在或尚未发布"
          description="它可能已被删除，或者管理员还没有发布这条公告。"
        >
          <RouterLink class="gcc-btn gcc-btn-primary" :to="`/community/${detail.project.slug}`">
            返回首页
          </RouterLink>
        </PublicStateBlock>

        <PublicStateBlock
          v-else-if="errorMessage"
          variant="error"
          kicker="ERROR · NEWS"
          title="公告加载失败"
          :description="errorMessage"
          @retry="loadAnnouncement"
        />

        <article v-else-if="announcement" class="news-detail__article">
          <header class="news-detail__head">
            <div class="news-detail__labels">
              <span v-if="announcement.pinned" class="news-detail__pin">PINNED</span>
              <span class="news-detail__date">{{ formatDateTime(announcement.publishedAt) }}</span>
            </div>
            <h1 class="news-detail__title">{{ announcement.title }}</h1>
          </header>

          <div class="news-detail__body">
            <MarkdownRenderer
              :markdown="announcement.bodyMarkdown || announcement.content"
              empty-text="暂无公告正文"
            />
          </div>
        </article>
      </div>
    </template>
  </PublicPageShell>
</template>

<style scoped>
.news-detail {
  padding: 36px 0 96px;
}

.news-detail__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 22px;
  margin-bottom: 40px;
  border-bottom: 1px solid var(--gcc-border);
}

.news-detail__back {
  font-family: var(--gcc-mono);
  font-size: 10.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  transition: color 0.15s ease;
}

.news-detail__back:hover {
  color: var(--gcc-text);
}

.news-detail__kicker {
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.18em;
  color: var(--gcc-muted);
  opacity: 0.8;
}

.news-detail__article {
  max-width: 920px;
}

.news-detail__head {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 30px;
  border-bottom: 1px solid var(--gcc-border);
}

.news-detail__labels {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.news-detail__pin {
  padding: 3px 9px;
  border-radius: 9999px;
  border: 1px solid var(--gcc-border-strong);
  color: var(--gcc-accent);
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
}

.news-detail__date {
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

.news-detail__title {
  color: var(--gcc-text);
  font-size: clamp(28px, 3.2vw, 44px);
  font-weight: 400;
  line-height: 1.08;
  letter-spacing: -0.025em;
}

.news-detail__body {
  padding-top: 30px;
  max-width: 860px;
  color: var(--gcc-text);
  font-size: 15px;
  word-break: break-word;
}

@media (max-width: 720px) {
  .news-detail {
    padding: 24px 0 64px;
  }

  .news-detail__topbar {
    margin-bottom: 24px;
  }

  .news-detail__title {
    font-size: 24px;
  }
}
</style>
