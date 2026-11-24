<script setup lang="ts">
/**
 * notice_list — 公告列表（PAGE_SCHEMA §2.3）
 *
 * - limit 生效：只展示组件自己声明的条数（同页多个公告组件共享一次请求）
 * - showPinned 为 true 时置顶公告排在前面（后端已排序，这里再做一次稳定排序兜底）并展示「置顶」标签
 * - 条目可点击/键盘 Enter → { name:'page-news-detail', params:{ slug, announcementId:id } }（统一 contentTargets）
 *
 * 视觉：hairline 行式列表（mono 日期 + 标题 + 摘要 + PINNED 标签），不用卡片堆叠。
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { formatDateTime } from '@/utils/format'
import { markdownToPlainText } from '@/utils/markdown'
import type { Announcement } from '@/types/api'
import type { RuntimeComponentProps } from '@/types/runtime'
import {
  asAnnouncementList,
  readBoolean,
  readBoundedNumber,
} from '@/runtime/componentProps'
import { injectRuntimeMode } from '@/runtime/context'
import { navigateToAnnouncement } from '@/runtime/contentTargets'
import { pushToast } from '@/runtime/toast'
import SectionHead from './SectionHead.vue'
import RuntimeState from './RuntimeState.vue'

const props = defineProps<RuntimeComponentProps>()

const router = useRouter()
const mode = injectRuntimeMode()

const limit = computed(() => readBoundedNumber(props.component, 'limit', 3, 1, 10))
const showPinned = computed(() => readBoolean(props.component, 'showPinned', true))
const canNavigate = computed(() => mode.value === 'public' && Boolean(props.site.projectSlug))

const items = computed<Announcement[]>(() => {
  const list = asAnnouncementList(props.data).slice()
  if (showPinned.value) {
    // 稳定排序（ES2019 起 Array#sort 为稳定排序）：置顶在前，其余保持后端顺序
    list.sort((left, right) => Number(right.pinned) - Number(left.pinned))
  }
  return list.slice(0, limit.value)
})

function pinnedBadgeVisible(item: Announcement): boolean {
  return showPinned.value && item.pinned
}

function contentSummary(item: Announcement): string {
  // 公告正文是 Markdown（v3）：列表摘要先转纯文本，避免把标记语法当正文展示
  return markdownToPlainText(item.bodyMarkdown || item.content, 140)
}

function openDetail(item: Announcement): void {
  if (!canNavigate.value) {
    pushToast('预览模式：公告详情页在公开站点查看')
    return
  }
  navigateToAnnouncement(router, props.site, item.id)
}

function onKeydown(event: KeyboardEvent, item: Announcement): void {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    openDetail(item)
  }
}
</script>

<template>
  <section class="runtime-notices" :aria-label="component.title">
    <div class="gcc-container">
      <SectionHead :title="component.title" kicker="ANNOUNCEMENTS" />

      <RuntimeState
        :status="status"
        :error-message="errorMessage"
        :retry="retry"
        loading-text="正在加载公告…"
        empty-text="暂无公告"
      >
        <ul class="notice-list">
          <li
            v-for="item in items"
            :key="item.id"
            class="notice-item"
            :class="{
              'notice-item--pinned': pinnedBadgeVisible(item),
              'notice-item--clickable': canNavigate,
            }"
            :role="canNavigate ? 'button' : undefined"
            :tabindex="canNavigate ? 0 : undefined"
            :aria-label="canNavigate ? `查看公告：${item.title}` : undefined"
            @click="openDetail(item)"
            @keydown="onKeydown($event, item)"
          >
            <time class="notice-item__date">{{ formatDateTime(item.publishedAt) }}</time>
            <div class="notice-item__main">
              <h3 class="notice-item__title">{{ item.title }}</h3>
              <p v-if="contentSummary(item)" class="notice-item__summary">
                {{ contentSummary(item) }}
              </p>
            </div>
            <span v-if="pinnedBadgeVisible(item)" class="notice-item__pin">PINNED</span>
          </li>
        </ul>
      </RuntimeState>
    </div>
  </section>
</template>

<style scoped>
.runtime-notices {
  padding: 64px 0 8px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}

.notice-item {
  display: grid;
  grid-template-columns: 148px minmax(0, 1fr) auto;
  align-items: start;
  gap: 20px;
  padding: 20px 14px;
  border-bottom: 1px solid var(--gcc-border);
  transition: background 0.15s ease;
}

.notice-item:first-child {
  border-top: 1px solid var(--gcc-border);
}

.notice-item--clickable {
  cursor: pointer;
}

.notice-item--clickable:hover {
  background: var(--gcc-hover);
}

.notice-item--clickable:focus-visible {
  outline: 2px solid var(--gcc-primary);
  outline-offset: -2px;
}

.notice-item__date {
  padding-top: 2px;
  font-family: var(--gcc-mono);
  font-size: 10.5px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  white-space: nowrap;
}

.notice-item__main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.notice-item__title {
  color: var(--gcc-text);
  font-size: 16px;
  font-weight: 500;
  line-height: 1.45;
  letter-spacing: -0.01em;
}

.notice-item__summary {
  color: var(--gcc-muted);
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notice-item__pin {
  flex: none;
  padding: 3px 9px;
  border-radius: 9999px;
  border: 1px solid var(--gcc-border-strong);
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--gcc-accent);
  white-space: nowrap;
}

@media (max-width: 720px) {
  .notice-item {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .notice-item__date {
    grid-column: 1 / -1;
  }
}
</style>
