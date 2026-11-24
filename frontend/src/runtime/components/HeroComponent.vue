<script setup lang="ts">
/**
 * hero — 游戏介绍横幅（PAGE_SCHEMA §2.1）
 *
 * - headline 优先，为空回退 gameInfo.name
 * - subheadline 为副标题；backgroundImage 为空时使用主题色世界感渐变
 * - showGameName 控制游戏名是否显式展示；align 控制对齐
 * - 数据由 CommunityRuntime 统一加载；本组件只按 status 展示四态
 *
 * 视觉：xAI 门户 Hero —— mono kicker + 大号 display 标题（weight 400 / 负字距）+
 * 黑色渐变遮罩（有背景图时）+ mono 元信息标签；颜色取自门户主题 --gcc-*。
 */
import { computed } from 'vue'
import type { GameInfo } from '@/types/api'
import type { ComponentStatus, RuntimeComponentProps } from '@/types/runtime'
import { asGameInfo, readBoolean, readString } from '@/runtime/componentProps'
import RuntimeState from './RuntimeState.vue'

const props = defineProps<RuntimeComponentProps>()

const gameInfo = computed<GameInfo | null>(() => asGameInfo(props.data))

/** ready 但没有可用游戏信息时，按空态处理（不展示空白横幅） */
const viewStatus = computed<ComponentStatus>(() =>
  props.status === 'ready' && !gameInfo.value ? 'empty' : props.status,
)

const headline = computed(() => readString(props.component, 'headline').trim())
const subheadline = computed(() => readString(props.component, 'subheadline').trim())
const showGameName = computed(() => readBoolean(props.component, 'showGameName', true))
const align = computed(() =>
  readString(props.component, 'align', 'center') === 'left' ? 'left' : 'center',
)

const displayTitle = computed(() => headline.value || gameInfo.value?.name || '')

/** 仅接受 http(s) 图片地址，其他字符按空值回退主题渐变 */
const backgroundUrl = computed(() => {
  const raw = readString(props.component, 'backgroundImage').trim()
  return /^https?:\/\//i.test(raw) ? raw : ''
})

const heroStyle = computed<Record<string, string>>(() => {
  if (backgroundUrl.value) {
    const style: Record<string, string> = {
      backgroundImage: `linear-gradient(180deg, rgba(4, 6, 12, 0.32) 0%, rgba(4, 6, 12, 0.62) 52%, var(--gcc-bg) 100%), url("${backgroundUrl.value}")`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
    }
    return style
  }
  const style: Record<string, string> = {
    backgroundImage: [
      'radial-gradient(1100px 460px at 16% -24%, color-mix(in srgb, var(--gcc-primary) 24%, transparent), transparent 62%)',
      'radial-gradient(820px 380px at 94% -30%, color-mix(in srgb, var(--gcc-accent) 18%, transparent), transparent 58%)',
      'linear-gradient(180deg, transparent 30%, var(--gcc-bg) 100%)',
    ].join(', '),
  }
  return style
})

/** 用真实 game-info 字段做 mono 元信息标签（平台 / 发行状态），无数据时整行不渲染 */
const metaChips = computed(() => {
  const info = gameInfo.value
  if (!info) return []
  return [info.platforms, info.releaseStatus]
    .map((value) => (typeof value === 'string' ? value.trim() : ''))
    .filter((value) => value.length > 0)
})
</script>

<template>
  <section
    class="runtime-hero"
    :class="[`runtime-hero--${align}`, { 'runtime-hero--image': Boolean(backgroundUrl) }]"
    :style="heroStyle"
    :aria-label="component.title"
  >
    <div class="runtime-hero__inner gcc-container">
      <p class="runtime-hero__kicker">{{ component.title }}</p>

      <RuntimeState
        v-if="viewStatus !== 'ready'"
        :status="viewStatus"
        :error-message="errorMessage"
        :retry="retry"
        loading-text="正在加载游戏信息…"
        empty-text="暂无游戏信息"
      />

      <template v-else>
        <p v-if="showGameName && headline" class="runtime-hero__game-name">{{ gameInfo?.name }}</p>
        <h1 class="runtime-hero__headline">{{ displayTitle }}</h1>
        <p v-if="subheadline" class="runtime-hero__subheadline">{{ subheadline }}</p>
        <div v-if="metaChips.length" class="runtime-hero__meta">
          <span v-for="chip in metaChips" :key="chip" class="runtime-hero__chip">{{ chip }}</span>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.runtime-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  min-height: 440px;
  padding: 96px 0 84px;
  border-bottom: 1px solid var(--gcc-border);
  background-color: var(--gcc-bg);
}

.runtime-hero--image {
  min-height: 520px;
}

.runtime-hero__inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: 100%;
}

.runtime-hero--center .runtime-hero__inner {
  align-items: center;
  text-align: center;
}

.runtime-hero--left .runtime-hero__inner {
  align-items: flex-start;
  text-align: left;
}

.runtime-hero__kicker {
  display: inline-flex;
  align-self: inherit;
  padding: 4px 12px;
  border-radius: 9999px;
  border: 1px solid var(--gcc-border-strong);
  background: var(--gcc-hover);
  color: var(--gcc-muted);
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.runtime-hero__game-name {
  color: var(--gcc-muted);
  font-family: var(--gcc-mono);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.runtime-hero__headline {
  max-width: 960px;
  color: var(--gcc-text);
  font-size: clamp(40px, 5.6vw, 76px);
  font-weight: 400;
  line-height: 1.02;
  letter-spacing: -0.035em;
}

.runtime-hero__subheadline {
  max-width: 680px;
  color: var(--gcc-text-secondary, var(--gcc-muted));
  font-size: 16.5px;
  line-height: 1.75;
}

.runtime-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
}

.runtime-hero__chip {
  padding: 4px 11px;
  border-radius: 9999px;
  border: 1px solid var(--gcc-border-strong);
  color: var(--gcc-text-secondary, var(--gcc-text));
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

/* 有背景图时使用固定浅色文字，保证深色遮罩上可读 */
.runtime-hero--image .runtime-hero__headline {
  color: #ffffff;
}

.runtime-hero--image .runtime-hero__subheadline,
.runtime-hero--image .runtime-hero__game-name {
  color: rgba(255, 255, 255, 0.82);
}

.runtime-hero--image .runtime-hero__kicker {
  color: rgba(255, 255, 255, 0.86);
  border-color: rgba(255, 255, 255, 0.28);
  background: rgba(4, 6, 12, 0.32);
}

.runtime-hero--image .runtime-hero__chip {
  color: rgba(255, 255, 255, 0.86);
  border-color: rgba(255, 255, 255, 0.28);
}

.runtime-hero__inner :deep(.runtime-state) {
  max-width: 680px;
}

@media (max-width: 768px) {
  .runtime-hero {
    min-height: 340px;
    padding: 72px 0 60px;
  }

  .runtime-hero--image {
    min-height: 420px;
  }
}
</style>
