<script setup lang="ts">
/**
 * links_grid — 友情链接（PAGE_SCHEMA §2.11）。
 *
 * 数据由 CommunityRuntime 统一加载（links 数据源；公开端只返回启用项）。
 * 外链必须带 rel="noopener noreferrer"，非法 http(s) 地址渲染为不可点击文本。
 *
 * 视觉：深色 hairline 卡片入口（名称 + 简介 + mono URL，hover 边框变亮）。
 */
import { computed } from 'vue'
import type { FriendLinkView } from '@/types/content'
import type { RuntimeComponentProps } from '@/types/runtime'
import {
  asLinkList,
  readBoolean,
  readBoundedNumber,
  readHttpUrl,
  summarize,
} from '@/runtime/componentProps'
import SectionHead from './SectionHead.vue'
import RuntimeState from './RuntimeState.vue'

const props = defineProps<RuntimeComponentProps>()

const columns = computed(() => readBoundedNumber(props.component, 'columns', 3, 1, 4))
const showDescription = computed(() => readBoolean(props.component, 'showDescription', true))
const items = computed<FriendLinkView[]>(() => asLinkList(props.data))

const gridStyle = computed<Record<string, string>>(() => ({
  gridTemplateColumns: `repeat(${columns.value}, minmax(0, 1fr))`,
}))

function linkUrl(link: FriendLinkView): string {
  return readHttpUrl(link.url)
}

function descriptionOf(link: FriendLinkView): string {
  return summarize(link.description, 120)
}
</script>

<template>
  <section class="runtime-links" :aria-label="component.title">
    <div class="gcc-container">
      <SectionHead :title="component.title" kicker="LINKS / ELSEWHERE" />

      <RuntimeState
        :status="status"
        :error-message="errorMessage"
        :retry="retry"
        loading-text="正在加载友情链接…"
        empty-text="暂无友情链接"
      >
        <ul class="links-grid" :style="gridStyle">
          <li v-for="link in items" :key="link.id" class="links-card">
            <component
              :is="linkUrl(link) ? 'a' : 'div'"
              class="links-card__body"
              :class="{ 'links-card__body--external': Boolean(linkUrl(link)) }"
              :href="linkUrl(link) || undefined"
              :target="linkUrl(link) ? '_blank' : undefined"
              :rel="linkUrl(link) ? 'noopener noreferrer' : undefined"
            >
              <div class="links-card__head">
                <h3 class="links-card__name">{{ link.name }}</h3>
                <span v-if="linkUrl(link)" class="links-card__arrow" aria-hidden="true">↗</span>
              </div>
              <p v-if="showDescription && descriptionOf(link)" class="links-card__desc">
                {{ descriptionOf(link) }}
              </p>
              <p v-if="linkUrl(link)" class="links-card__url">{{ linkUrl(link) }}</p>
            </component>
          </li>
        </ul>
      </RuntimeState>
    </div>
  </section>
</template>

<style scoped>
.runtime-links {
  padding: 64px 0 8px;
}

.links-grid {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.links-card {
  display: flex;
}

.links-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  height: 100%;
  padding: 22px;
  border-radius: var(--gcc-card-radius, 8px);
  border: 1px solid var(--gcc-border);
  background: var(--gcc-card-bg);
  color: var(--gcc-text);
  transition: border-color 0.15s ease;
}

.links-card__body--external {
  cursor: pointer;
}

.links-card__body--external:hover {
  border-color: var(--gcc-border-strong);
}

.links-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.links-card__name {
  color: var(--gcc-text);
  font-size: 16px;
  font-weight: 500;
  line-height: 1.4;
  letter-spacing: -0.01em;
}

.links-card__arrow {
  color: var(--gcc-muted);
  font-size: 14px;
  transition: color 0.15s ease;
}

.links-card__body--external:hover .links-card__arrow {
  color: var(--gcc-text);
}

.links-card__desc {
  color: var(--gcc-muted);
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.links-card__url {
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--gcc-border);
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--gcc-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .links-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
  }
}

@media (max-width: 560px) {
  .links-grid {
    grid-template-columns: minmax(0, 1fr) !important;
  }
}
</style>
