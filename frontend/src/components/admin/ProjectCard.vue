<script setup lang="ts">
/**
 * 游戏站点卡片（我的项目）：封面艺术 + 名称/状态/页面数/更新时间 + 操作。
 * 封面使用 CSS 生成的克制深色艺术（无外部图片、无大色块）。
 */
import { computed } from 'vue'
import XIcon from '@/components/admin/XIcon.vue'
import type { ProjectView } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    project: ProjectView
    busy?: boolean
    /** compact 用于总览页：隐藏次要操作 */
    compact?: boolean
  }>(),
  { busy: false, compact: false },
)

const emit = defineEmits<{
  (event: 'open'): void
  (event: 'settings'): void
  (event: 'publish'): void
  (event: 'delete'): void
  (event: 'edit'): void
  (event: 'visit'): void
}>()

const palettes = [
  { a: 'rgba(255, 122, 23, 0.16)', b: 'rgba(124, 58, 237, 0.12)', line: 'rgba(255, 122, 23, 0.35)' },
  { a: 'rgba(160, 195, 236, 0.14)', b: 'rgba(124, 58, 237, 0.14)', line: 'rgba(160, 195, 236, 0.32)' },
  { a: 'rgba(196, 181, 253, 0.13)', b: 'rgba(61, 220, 151, 0.1)', line: 'rgba(196, 181, 253, 0.32)' },
  { a: 'rgba(61, 220, 151, 0.1)', b: 'rgba(255, 122, 23, 0.12)', line: 'rgba(61, 220, 151, 0.3)' },
]

function hash(value: string): number {
  let result = 0
  for (let index = 0; index < value.length; index += 1) {
    result = (result * 31 + value.charCodeAt(index)) % 997
  }
  return result
}

const palette = computed(() => palettes[hash(props.project.slug) % palettes.length])
const isPublished = computed(() => props.project.status === 'PUBLISHED')
const initials = computed(() =>
  props.project.name
    .replace(/[^\p{L}\p{N}]/gu, '')
    .slice(0, 2)
    .toUpperCase(),
)
const updatedLabel = computed(() => formatDateTime(props.project.updatedAt))
const pageProgress = computed(() =>
  props.project.pageCount > 0
    ? `${props.project.publishedPageCount}/${props.project.pageCount}`
    : '0/0',
)

const coverStyle = computed(() => ({
  '--cover-a': palette.value.a,
  '--cover-b': palette.value.b,
  '--cover-line': palette.value.line,
}))
</script>

<template>
  <article class="project-card" :class="{ 'is-busy': busy }">
    <div class="project-card__cover" :style="coverStyle">
      <div class="project-card__cover-grid" aria-hidden="true"></div>
      <div class="project-card__cover-glow" aria-hidden="true"></div>
      <span class="project-card__initials x-mono" aria-hidden="true">{{ initials }}</span>
      <div class="project-card__cover-top">
        <span class="project-card__status" :class="{ 'is-published': isPublished }">
          <i class="x-dot" :class="isPublished ? 'x-dot--success' : 'x-dot--warning'"></i>
          {{ isPublished ? 'Published' : 'Draft' }}
        </span>
        <span class="project-card__slug x-mono">/{{ project.slug }}</span>
      </div>
    </div>

    <div class="project-card__body">
      <div class="project-card__heading">
        <h3 class="project-card__title">{{ project.name }}</h3>
        <p class="project-card__game x-mono">GAME · {{ project.gameName }}</p>
      </div>
      <p v-if="project.description" class="project-card__desc">{{ project.description }}</p>

      <dl class="project-card__meta">
        <div>
          <dt>PAGES</dt>
          <dd class="x-mono">{{ pageProgress }}</dd>
        </div>
        <div>
          <dt>UPDATED</dt>
          <dd class="x-mono">{{ updatedLabel }}</dd>
        </div>
        <div>
          <dt>ID</dt>
          <dd class="x-mono">#{{ project.id }}</dd>
        </div>
      </dl>

      <div class="project-card__actions">
        <button
          type="button"
          class="gcc-btn gcc-btn-sm gcc-btn-primary"
          :disabled="busy"
          @click="emit('open')"
        >
          页面管理
          <XIcon name="arrow-right" :size="13" />
        </button>
        <button
          type="button"
          class="gcc-btn gcc-btn-sm"
          :disabled="busy"
          :title="`发布站点下所有有更新的页面（草稿 v${project.draftVersion} / 发布 v${project.publishedVersion ?? '—'}）`"
          @click="emit('publish')"
        >
          <XIcon name="publish" :size="13" />
          站点发布
        </button>
        <button type="button" class="gcc-btn gcc-btn-sm gcc-btn-ghost" @click="emit('settings')">
          设置
        </button>
        <span class="project-card__actions-spacer"></span>
        <button
          type="button"
          class="gcc-btn gcc-btn-sm gcc-btn-ghost"
          title="访问公开站点"
          aria-label="访问公开站点"
          @click="emit('visit')"
        >
          <XIcon name="external" :size="13" />
        </button>
        <button
          type="button"
          class="gcc-btn gcc-btn-sm gcc-btn-ghost"
          title="编辑站点信息"
          aria-label="编辑站点信息"
          @click="emit('edit')"
        >
          <XIcon name="edit" :size="13" />
        </button>
        <button
          type="button"
          class="gcc-btn gcc-btn-sm gcc-btn-danger"
          :disabled="busy"
          @click="emit('delete')"
        >
          <XIcon name="trash" :size="13" />
          删除站点
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.project-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
  transition: border-color 0.18s ease;
}

.project-card:hover {
  border-color: var(--x-border-strong);
}

.project-card.is-busy {
  opacity: 0.72;
  pointer-events: none;
}

/* ---------------- cover ---------------- */
.project-card__cover {
  position: relative;
  height: 148px;
  overflow: hidden;
  border-bottom: 1px solid var(--x-border);
  background:
    radial-gradient(120% 160% at 12% 0%, var(--cover-a), transparent 60%),
    radial-gradient(120% 160% at 88% 100%, var(--cover-b), transparent 62%),
    #0c0c0e;
}

.project-card__cover-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.85), transparent 90%);
}

.project-card__cover-glow {
  position: absolute;
  left: 26px;
  bottom: -46px;
  width: 180px;
  height: 120px;
  border-radius: 50%;
  background: var(--cover-line);
  filter: blur(52px);
  opacity: 0.6;
}

.project-card__initials {
  position: absolute;
  right: 22px;
  bottom: 4px;
  font-size: 64px;
  line-height: 1;
  font-weight: 500;
  letter-spacing: -0.04em;
  color: rgba(255, 255, 255, 0.08);
  user-select: none;
}

.project-card__cover-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
}

.project-card__status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.project-card__status.is-published {
  color: #b9f5da;
}

.project-card__slug {
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: var(--x-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 45%;
}

/* ---------------- body ---------------- */
.project-card__body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 18px 16px;
}

.project-card__heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.project-card__title {
  font-size: 17px;
  font-weight: 500;
  letter-spacing: -0.01em;
  color: var(--x-text);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-card__game {
  flex: none;
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--x-muted);
  text-transform: uppercase;
}

.project-card__desc {
  min-height: 20px;
  color: var(--x-muted);
  font-size: 12.5px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-card__meta {
  display: flex;
  align-items: center;
  gap: 22px;
  margin: 0;
  padding-top: 2px;
}

.project-card__meta > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.project-card__meta dt {
  font-family: var(--x-mono);
  font-size: 8.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--x-muted);
  opacity: 0.75;
}

.project-card__meta dd {
  margin: 0;
  font-size: 10.5px;
  letter-spacing: 0.04em;
  color: var(--x-text-secondary);
}

.project-card__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px solid var(--x-border);
  flex-wrap: wrap;
}

.project-card__actions-spacer {
  flex: 1;
  min-width: 4px;
}

@media (max-width: 620px) {
  .project-card__meta {
    gap: 16px;
    flex-wrap: wrap;
  }

  .project-card__actions {
    gap: 6px;
  }
}
</style>
