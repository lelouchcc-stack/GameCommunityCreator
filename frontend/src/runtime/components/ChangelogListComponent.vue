<script setup lang="ts">
/**
 * changelog_list — 更新日志列表（PAGE_SCHEMA §2.7）。
 *
 * 数据由 CommunityRuntime 统一加载（changelogs 数据源，预览只取 PUBLISHED）。
 * 卡片点击是**固定逻辑跳转**（不走按钮动作配置）：
 *   公开模式 → /community/{slug}/changelog/{id}
 *   编辑器 / 预览模式 → 提示详情页在公开站点查看，不离开管理端。
 *
 * 视觉：软件 Release Notes 排版 —— mono 大号版本号 + 发布日期 + 标题 + 摘要，hairline 分隔。
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { ChangelogView } from '@/types/content'
import type { RuntimeComponentProps } from '@/types/runtime'
import { formatDate } from '@/utils/format'
import { markdownToPlainText } from '@/utils/markdown'
import { asChangelogList, readBoolean, readBoundedNumber } from '@/runtime/componentProps'
import { injectRuntimeMode } from '@/runtime/context'
import { changelogListTarget, navigateToChangelog } from '@/runtime/contentTargets'
import { pushToast } from '@/runtime/toast'
import SectionHead from './SectionHead.vue'
import RuntimeState from './RuntimeState.vue'

const props = defineProps<RuntimeComponentProps>()

const router = useRouter()
const mode = injectRuntimeMode()

const limit = computed(() => readBoundedNumber(props.component, 'limit', 6, 1, 20))
const showVersion = computed(() => readBoolean(props.component, 'showVersion', true))

const items = computed<ChangelogView[]>(() => asChangelogList(props.data).slice(0, limit.value))

const canNavigate = computed(() => mode.value === 'public' && Boolean(props.site.projectSlug))
const listTarget = computed(() => changelogListTarget(props.site))

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

function openDetail(item: ChangelogView): void {
  if (!canNavigate.value) {
    pushToast('预览模式：更新日志详情页在公开站点查看')
    return
  }
  navigateToChangelog(router, props.site, item.id)
}

function onKeydown(event: KeyboardEvent, item: ChangelogView): void {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    openDetail(item)
  }
}

function summaryOf(item: ChangelogView): string {
  // 正文是 Markdown（v3）：列表摘要转纯文本
  return markdownToPlainText(item.bodyMarkdown || item.content, 120)
}
</script>

<template>
  <section class="runtime-changelog" :aria-label="component.title">
    <div class="gcc-container">
      <SectionHead :title="component.title" kicker="RELEASE NOTES">
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
        loading-text="正在加载更新日志…"
        empty-text="暂无更新日志"
      >
        <ul class="changelog-list">
          <li v-for="item in items" :key="item.id">
            <article
              class="changelog-row"
              :class="{ 'changelog-row--clickable': canNavigate }"
              :role="canNavigate ? 'link' : undefined"
              :tabindex="canNavigate ? 0 : undefined"
              @click="openDetail(item)"
              @keydown="onKeydown($event, item)"
            >
              <div class="changelog-row__version">
                <span v-if="showVersion && item.version" class="changelog-row__version-text">
                  {{ item.version }}
                </span>
                <time class="changelog-row__date">{{ formatDate(item.releaseDate) }}</time>
              </div>
              <div class="changelog-row__main">
                <h3 class="changelog-row__title">{{ item.title }}</h3>
                <p v-if="summaryOf(item)" class="changelog-row__summary">{{ summaryOf(item) }}</p>
              </div>
              <span class="changelog-row__arrow" aria-hidden="true">→</span>
            </article>
          </li>
        </ul>
      </RuntimeState>
    </div>
  </section>
</template>

<style scoped>
.runtime-changelog {
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

.changelog-list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}

.changelog-row {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr) auto;
  align-items: start;
  gap: 24px;
  padding: 24px 12px;
  border-bottom: 1px solid var(--gcc-border);
  transition: background 0.15s ease;
}

.changelog-row:first-child {
  border-top: 1px solid var(--gcc-border);
}

.changelog-row--clickable {
  cursor: pointer;
}

.changelog-row--clickable:hover {
  background: var(--gcc-hover);
}

.changelog-row--clickable:focus-visible {
  outline: 2px solid var(--gcc-primary);
  outline-offset: -2px;
}

.changelog-row__version {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.changelog-row__version-text {
  font-family: var(--gcc-mono);
  font-size: 20px;
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--gcc-text);
}

.changelog-row__date {
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

.changelog-row__main {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
}

.changelog-row__title {
  color: var(--gcc-text);
  font-size: 16.5px;
  font-weight: 500;
  line-height: 1.45;
  letter-spacing: -0.01em;
}

.changelog-row__summary {
  color: var(--gcc-muted);
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.changelog-row__arrow {
  align-self: center;
  color: var(--gcc-muted);
  font-size: 15px;
  opacity: 0.7;
  transition:
    color 0.15s ease,
    opacity 0.15s ease;
}

.changelog-row--clickable:hover .changelog-row__arrow {
  color: var(--gcc-text);
  opacity: 1;
}

@media (max-width: 720px) {
  .changelog-row {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 12px;
  }

  .changelog-row__version {
    grid-column: 1 / -1;
    flex-direction: row;
    align-items: baseline;
    gap: 12px;
  }

  .changelog-row__arrow {
    align-self: start;
  }
}
</style>
