<script setup lang="ts">
/**
 * issue_progress — 问题处理进展（v3.2 新增，PAGE_SCHEMA §2.12）。
 *
 * 数据来自真实问题反馈：只展示「已解决（RESOLVED）且允许公开（isPublic）」的问题，
 * 管理端预览走管理端列表接口并由 runtime/loaders.ts 过滤 isPublic。
 *
 * 卡片点击是固定逻辑跳转（不走按钮动作配置）：
 *   公开模式 → { name:'page-progress', params:{ slug }, hash:'#issue-{id}' }
 *   编辑器 / 预览模式 → 提示在公开站点查看。
 *
 * 视觉：xAI hairline 行式列表 —— mono 状态/分类/时间 + 标题 + 描述摘要 + 处理结果摘要。
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { IssueProgressItem } from '@/types/content'
import type { RuntimeComponentProps } from '@/types/runtime'
import { formatDateTime } from '@/utils/format'
import {
  asIssueProgressList,
  readBoundedNumber,
  readTrimmed,
  summarize,
} from '@/runtime/componentProps'
import { injectRuntimeMode } from '@/runtime/context'
import { navigateToIssueProgress } from '@/runtime/contentTargets'
import { pushToast } from '@/runtime/toast'
import SectionHead from './SectionHead.vue'
import RuntimeState from './RuntimeState.vue'

const props = defineProps<RuntimeComponentProps>()

const router = useRouter()
const mode = injectRuntimeMode()

const limit = computed(() => readBoundedNumber(props.component, 'limit', 5, 1, 20))
const emptyText = computed(() =>
  readTrimmed(props.component, 'emptyText', '暂无公开的处理进展，问题解决后会在官网上同步。'),
)
const items = computed<IssueProgressItem[]>(() =>
  asIssueProgressList(props.data).slice(0, limit.value),
)

const canNavigate = computed(() => mode.value === 'public' && Boolean(props.site.projectSlug))

function openDetail(item: IssueProgressItem): void {
  if (!canNavigate.value) {
    pushToast('预览模式：问题处理进展页在公开站点查看')
    return
  }
  navigateToIssueProgress(router, props.site, item.id)
}

function onKeydown(event: KeyboardEvent, item: IssueProgressItem): void {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    openDetail(item)
  }
}

function descriptionOf(item: IssueProgressItem): string {
  return summarize(item.description, 140)
}

function resolutionOf(item: IssueProgressItem): string {
  return summarize(item.resolution, 140)
}
</script>

<template>
  <section class="issue-progress runtime-progress" :aria-label="component.title">
    <div class="gcc-container">
      <SectionHead :title="component.title" kicker="ISSUE PROGRESS / RESOLVED" />

      <RuntimeState
        :status="status"
        :error-message="errorMessage"
        :retry="retry"
        loading-text="正在加载问题处理进展…"
        :empty-text="emptyText"
      >
        <ul class="progress-list">
          <li v-for="item in items" :key="item.id">
            <article
              class="issue-progress-item progress-row"
              :class="{ 'progress-row--clickable': canNavigate }"
              :data-issue-id="item.id"
              :role="canNavigate ? 'link' : undefined"
              :tabindex="canNavigate ? 0 : undefined"
              @click="openDetail(item)"
              @keydown="onKeydown($event, item)"
            >
              <div class="progress-row__head">
                <span class="progress-row__status">RESOLVED</span>
                <span class="progress-row__category">{{ item.category || '未分类' }}</span>
                <time class="progress-row__date">{{ formatDateTime(item.updatedAt) }}</time>
              </div>
              <h3 class="progress-row__title">{{ item.title }}</h3>
              <p v-if="descriptionOf(item)" class="progress-row__desc">
                {{ descriptionOf(item) }}
              </p>
              <p v-if="resolutionOf(item)" class="progress-row__resolution">
                <span class="progress-row__label">处理结果</span>
                {{ resolutionOf(item) }}
              </p>
            </article>
          </li>
        </ul>
      </RuntimeState>
    </div>
  </section>
</template>

<style scoped>
.runtime-progress {
  padding: 64px 0 8px;
}

.progress-list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}

.progress-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 22px 14px;
  border-bottom: 1px solid var(--gcc-border);
  transition: background 0.15s ease;
}

.progress-row:first-child {
  border-top: 1px solid var(--gcc-border);
}

.progress-row--clickable {
  cursor: pointer;
}

.progress-row--clickable:hover {
  background: var(--gcc-hover);
}

.progress-row--clickable:focus-visible {
  outline: 2px solid var(--gcc-primary);
  outline-offset: -2px;
}

.progress-row__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.progress-row__status {
  flex: none;
  padding: 3px 9px;
  border-radius: 9999px;
  border: 1px solid rgba(61, 220, 151, 0.4);
  background: rgba(61, 220, 151, 0.08);
  color: var(--gcc-success, #3ddc97);
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
}

.progress-row__category {
  padding: 3px 9px;
  border-radius: 9999px;
  border: 1px solid var(--gcc-border-strong);
  color: var(--gcc-text-secondary, var(--gcc-text));
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.progress-row__date {
  margin-left: auto;
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  white-space: nowrap;
}

.progress-row__title {
  color: var(--gcc-text);
  font-size: 16.5px;
  font-weight: 500;
  line-height: 1.45;
  letter-spacing: -0.01em;
}

.progress-row__desc {
  color: var(--gcc-muted);
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.progress-row__resolution {
  display: flex;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--gcc-border);
  color: var(--gcc-text-secondary, var(--gcc-text));
  font-size: 13px;
  line-height: 1.7;
}

.progress-row__label {
  flex: none;
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  padding-top: 3px;
}

@media (max-width: 720px) {
  .progress-row__date {
    margin-left: 0;
  }
}
</style>
