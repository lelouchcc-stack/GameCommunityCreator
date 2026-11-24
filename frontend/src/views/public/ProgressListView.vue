<script setup lang="ts">
/**
 * 问题处理进展列表（/community/:slug/progress，路由 page-progress）—— 固定视图。
 *
 * 数据：GET /api/public/communities/{slug}/issues/progress（已解决 + 允许公开的问题）。
 * 支持从首页「问题处理进展」卡片跳转时携带 `#issue-{id}` 锚点并自动定位到对应条目。
 */
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import PublicStateBlock from '@/components/public/PublicStateBlock.vue'
import PublicPageShell from '@/runtime/PublicPageShell.vue'
import { toErrorMessage } from '@/runtime/errors'
import { createPublicLoader } from '@/runtime/loaders'
import type { IssueProgressItem } from '@/types/content'
import { formatDateTime } from '@/utils/format'

const route = useRoute()

const slug = computed(() => {
  const raw = route.params.slug
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' ? value : ''
})

const items = ref<IssueProgressItem[]>([])
const loading = ref(true)
const errorMessage = ref('')

let loadSeq = 0

async function loadProgress(): Promise<void> {
  const seq = loadSeq + 1
  loadSeq = seq
  loading.value = true
  errorMessage.value = ''
  items.value = []

  if (!slug.value) {
    loading.value = false
    return
  }

  try {
    const loader = createPublicLoader(slug.value)
    const list = await loader.loadIssueProgress(50)
    if (seq !== loadSeq) return
    items.value = Array.isArray(list) ? list : []
  } catch (error) {
    if (seq !== loadSeq) return
    errorMessage.value = toErrorMessage(error, '问题处理进展加载失败，请稍后重试')
  } finally {
    if (seq === loadSeq) loading.value = false
  }
  await nextTick()
  scrollToHash()
}

/** 锚点定位（#issue-{id}）：DOM 可能比数据晚一步出现，最多重试 ~1.2s */
function scrollToHash(attempt = 0): void {
  if (typeof document === 'undefined') return
  const id = route.hash.replace(/^#/, '')
  if (!id) return
  const target = document.getElementById(id)
  if (target) {
    // jsdom / 极老浏览器可能没有 scrollIntoView
    if (typeof target.scrollIntoView === 'function') {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
    return
  }
  if (attempt < 12) {
    window.setTimeout(() => scrollToHash(attempt + 1), 100)
  }
}

watch(slug, () => void loadProgress(), { immediate: true })
watch(
  [items, () => route.hash],
  async () => {
    await nextTick()
    scrollToHash()
  },
)

onBeforeUnmount(() => {
  loadSeq += 1
})
</script>

<template>
  <PublicPageShell :slug="slug" page-slug="home" not-found-text="该页面不存在或尚未发布">
    <template #default="{ detail }">
      <div class="progress-page gcc-container">
        <div class="progress-page__topbar">
          <RouterLink class="progress-page__back" :to="`/community/${detail.project.slug}`">
            ← 返回首页
          </RouterLink>
          <span class="progress-page__kicker">ISSUE PROGRESS</span>
        </div>

        <header class="progress-page__head">
          <p class="progress-page__eyebrow">ISSUE PROGRESS / RESOLVED</p>
          <h1 class="progress-page__title">问题处理进展</h1>
          <p class="progress-page__desc">
            这里同步已解决且允许公开的玩家反馈与处理结果；共 {{ items.length }} 条。
          </p>
        </header>

        <PublicStateBlock
          v-if="loading"
          variant="loading"
          kicker="ISSUE PROGRESS"
          title="正在加载问题处理进展…"
        />

        <PublicStateBlock
          v-else-if="errorMessage"
          variant="error"
          kicker="ERROR · ISSUE PROGRESS"
          title="问题处理进展加载失败"
          :description="errorMessage"
          @retry="loadProgress"
        />

        <PublicStateBlock
          v-else-if="items.length === 0"
          variant="empty"
          kicker="NO RESOLVED ISSUES"
          title="暂无公开的问题处理进展"
          description="问题被处理并标记为可公开后，会出现在这里。"
        />

        <ul v-else class="progress-list">
          <li
            v-for="item in items"
            :id="`issue-${item.id}`"
            :key="item.id"
            class="progress-item"
          >
            <div class="progress-item__head">
              <span class="progress-item__status">RESOLVED</span>
              <span class="progress-item__category">{{ item.category || '未分类' }}</span>
              <time class="progress-item__date">{{ formatDateTime(item.updatedAt) }}</time>
            </div>
            <h2 class="progress-item__title">{{ item.title }}</h2>
            <div class="progress-item__section">
              <p class="progress-item__label">问题描述</p>
              <p class="progress-item__text">{{ item.description }}</p>
            </div>
            <div class="progress-item__section progress-item__section--resolution">
              <p class="progress-item__label">处理结果</p>
              <p class="progress-item__text">{{ item.resolution || '已处理。' }}</p>
            </div>
          </li>
        </ul>
      </div>
    </template>
  </PublicPageShell>
</template>

<style scoped>
.progress-page {
  padding: 36px 0 96px;
}

.progress-page__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 22px;
  margin-bottom: 40px;
  border-bottom: 1px solid var(--gcc-border);
}

.progress-page__back {
  font-family: var(--gcc-mono);
  font-size: 10.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  transition: color 0.15s ease;
}

.progress-page__back:hover {
  color: var(--gcc-text);
}

.progress-page__kicker {
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.18em;
  color: var(--gcc-muted);
  opacity: 0.8;
}

.progress-page__head {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 860px;
  padding-bottom: 30px;
}

.progress-page__eyebrow {
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

.progress-page__title {
  color: var(--gcc-text);
  font-size: clamp(30px, 3.6vw, 46px);
  font-weight: 400;
  line-height: 1.06;
  letter-spacing: -0.025em;
}

.progress-page__desc {
  color: var(--gcc-muted);
  font-size: 13.5px;
  line-height: 1.8;
}

.progress-list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}

.progress-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 26px 14px;
  border-bottom: 1px solid var(--gcc-border);
  /* sticky 门户外壳（64px）下的锚点偏移 */
  scroll-margin-top: 88px;
}

.progress-item:first-child {
  border-top: 1px solid var(--gcc-border);
}

.progress-item__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.progress-item__status {
  padding: 3px 9px;
  border-radius: 9999px;
  border: 1px solid rgba(61, 220, 151, 0.4);
  background: rgba(61, 220, 151, 0.08);
  color: var(--gcc-success, #3ddc97);
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
}

.progress-item__category {
  padding: 3px 9px;
  border-radius: 9999px;
  border: 1px solid var(--gcc-border-strong);
  color: var(--gcc-text-secondary, var(--gcc-text));
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.progress-item__date {
  margin-left: auto;
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  white-space: nowrap;
}

.progress-item__title {
  color: var(--gcc-text);
  font-size: 19px;
  font-weight: 500;
  line-height: 1.4;
  letter-spacing: -0.01em;
}

.progress-item__section {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 860px;
}

.progress-item__section--resolution {
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px solid var(--gcc-border);
}

.progress-item__label {
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

.progress-item__text {
  color: var(--gcc-text-secondary, var(--gcc-text));
  font-size: 14px;
  line-height: 1.85;
  white-space: pre-line;
}

@media (max-width: 720px) {
  .progress-page {
    padding: 24px 0 64px;
  }

  .progress-page__topbar {
    margin-bottom: 24px;
  }

  .progress-item__date {
    margin-left: 0;
  }
}
</style>
