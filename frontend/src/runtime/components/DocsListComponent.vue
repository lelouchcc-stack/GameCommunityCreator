<script setup lang="ts">
/**
 * docs_list — 公开文档列表（PAGE_SCHEMA §2.8）。
 *
 * 数据由 CommunityRuntime 统一加载（docs 数据源；同源多组件按 category 分别去重请求）。
 * 卡片点击是固定逻辑跳转：公开模式 → /community/{slug}/docs/{docSlug}。
 *
 * 视觉：深色 hairline 卡片 + category mono 标签 + 更新时间 + 纯文本摘要。
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { DocView } from '@/types/content'
import type { RuntimeComponentProps } from '@/types/runtime'
import { formatDate } from '@/utils/format'
import { markdownToPlainText } from '@/utils/markdown'
import { asDocList, readBoundedNumber } from '@/runtime/componentProps'
import { injectRuntimeMode } from '@/runtime/context'
import { docsListTarget, navigateToDoc } from '@/runtime/contentTargets'
import { pushToast } from '@/runtime/toast'
import SectionHead from './SectionHead.vue'
import RuntimeState from './RuntimeState.vue'

const props = defineProps<RuntimeComponentProps>()

const router = useRouter()
const mode = injectRuntimeMode()

const limit = computed(() => readBoundedNumber(props.component, 'limit', 8, 1, 20))
const items = computed<DocView[]>(() => asDocList(props.data).slice(0, limit.value))
const canNavigate = computed(
  () => mode.value === 'public' && Boolean(props.site.projectSlug),
)
const listTarget = computed(() => docsListTarget(props.site))

/** 列表页 href（路由未注册时安全降级为空，避免编辑器/测试环境崩溃） */
const listHref = computed<string>(() => {
  try {
    return router.resolve(listTarget.value).href
  } catch {
    return ''
  }
})

function openList(): void {
  if (!canNavigate.value) return
  void router.push(listTarget.value).catch(() => {
    /* 路由取消不提示 */
  })
}

function openDetail(item: DocView): void {
  if (!item.slug) return
  if (!canNavigate.value) {
    pushToast('预览模式：文档详情页在公开站点查看')
    return
  }
  navigateToDoc(router, props.site, item.slug)
}

function onKeydown(event: KeyboardEvent, item: DocView): void {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    openDetail(item)
  }
}

function summaryOf(item: DocView): string {
  // 正文是 Markdown（v3）：列表摘要转纯文本
  return markdownToPlainText(item.bodyMarkdown || item.content, 110)
}
</script>

<template>
  <section class="runtime-docs" :aria-label="component.title">
    <div class="gcc-container">
      <SectionHead :title="component.title" kicker="DOCUMENTS / GUIDES">
        <template #extra>
          <a
            v-if="canNavigate"
            class="runtime-section-link"
            :href="listHref || undefined"
            @click.prevent="openList"
          >
            查看全部 →
          </a>
        </template>
      </SectionHead>

      <RuntimeState
        :status="status"
        :error-message="errorMessage"
        :retry="retry"
        loading-text="正在加载公开文档…"
        empty-text="暂无公开文档"
      >
        <ul class="docs-grid">
          <li v-for="item in items" :key="item.id">
            <article
              class="docs-card"
              :class="{ 'docs-card--clickable': canNavigate }"
              :role="canNavigate ? 'link' : undefined"
              :tabindex="canNavigate ? 0 : undefined"
              @click="openDetail(item)"
              @keydown="onKeydown($event, item)"
            >
              <div class="docs-card__head">
                <span class="docs-card__category">{{ item.category || '未分类' }}</span>
                <time class="docs-card__date">{{ formatDate(item.updatedAt) }}</time>
              </div>
              <h3 class="docs-card__title">{{ item.title }}</h3>
              <p v-if="summaryOf(item)" class="docs-card__summary">{{ summaryOf(item) }}</p>
              <span v-if="canNavigate" class="docs-card__cta" aria-hidden="true">READ DOC →</span>
            </article>
          </li>
        </ul>
      </RuntimeState>
    </div>
  </section>
</template>

<style scoped>
.runtime-docs {
  padding: 64px 0 8px;
}

.runtime-section-link {
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  white-space: nowrap;
  transition: color 0.15s ease;
}

.runtime-section-link:hover {
  color: var(--gcc-text);
}

.docs-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.docs-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
  padding: 22px;
  border-radius: var(--gcc-card-radius, 8px);
  border: 1px solid var(--gcc-border);
  background: var(--gcc-card-bg);
  transition: border-color 0.15s ease;
}

.docs-card--clickable {
  cursor: pointer;
}

.docs-card--clickable:hover {
  border-color: var(--gcc-border-strong);
}

.docs-card--clickable:focus-visible {
  outline: 2px solid var(--gcc-primary);
  outline-offset: 2px;
}

.docs-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.docs-card__category {
  padding: 3px 9px;
  border-radius: 9999px;
  border: 1px solid var(--gcc-border-strong);
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-text-secondary, var(--gcc-text));
  white-space: nowrap;
}

.docs-card__date {
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  white-space: nowrap;
}

.docs-card__title {
  color: var(--gcc-text);
  font-size: 17px;
  font-weight: 500;
  line-height: 1.4;
  letter-spacing: -0.01em;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.docs-card__summary {
  color: var(--gcc-muted);
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.docs-card__cta {
  margin-top: auto;
  padding-top: 10px;
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  color: var(--gcc-muted);
  transition: color 0.15s ease;
}

.docs-card--clickable:hover .docs-card__cta {
  color: var(--gcc-text);
}

@media (max-width: 720px) {
  .docs-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
