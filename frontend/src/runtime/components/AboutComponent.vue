<script setup lang="ts">
/**
 * about — 游戏简介（PAGE_SCHEMA §2.2）
 *
 * - body 为空时回退 gameInfo.description
 * - showTagline 控制是否展示 gameInfo.tagline 一句话简介
 *
 * 视觉：hairline 大区块 + 左侧 mono kicker / 大号标题 + 右侧宽行距正文。
 */
import { computed } from 'vue'
import type { GameInfo } from '@/types/api'
import type { ComponentStatus, RuntimeComponentProps } from '@/types/runtime'
import { asGameInfo, readBoolean, readTrimmed } from '@/runtime/componentProps'
import SectionHead from './SectionHead.vue'
import RuntimeState from './RuntimeState.vue'

const props = defineProps<RuntimeComponentProps>()

const gameInfo = computed<GameInfo | null>(() => asGameInfo(props.data))
const viewStatus = computed<ComponentStatus>(() =>
  props.status === 'ready' && !gameInfo.value ? 'empty' : props.status,
)

const showTagline = computed(() => readBoolean(props.component, 'showTagline', true))
const tagline = computed(() => (gameInfo.value?.tagline ?? '').trim())
const bodyText = computed(() => {
  const configured = readTrimmed(props.component, 'body')
  if (configured) return configured
  return (gameInfo.value?.description ?? '').trim()
})
</script>

<template>
  <section class="runtime-about" :aria-label="component.title">
    <div class="gcc-container">
      <SectionHead :title="component.title" kicker="ABOUT / THE GAME" />

      <RuntimeState
        :status="viewStatus"
        :error-message="errorMessage"
        :retry="retry"
        loading-text="正在加载游戏简介…"
        empty-text="暂无游戏简介"
      >
        <div class="runtime-about__body">
          <p v-if="showTagline && tagline" class="runtime-about__tagline">{{ tagline }}</p>
          <p v-if="bodyText" class="runtime-about__text">{{ bodyText }}</p>
          <p v-else class="runtime-about__text runtime-about__text--muted">暂无游戏简介</p>
        </div>
      </RuntimeState>
    </div>
  </section>
</template>

<style scoped>
.runtime-about {
  padding: 64px 0 8px;
}

.runtime-about__body {
  max-width: 860px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.runtime-about__tagline {
  color: var(--gcc-text);
  font-size: clamp(19px, 1.9vw, 24px);
  font-weight: 400;
  line-height: 1.55;
  letter-spacing: -0.01em;
}

.runtime-about__text {
  color: var(--gcc-text-secondary, var(--gcc-text));
  font-size: 15px;
  line-height: 1.95;
  white-space: pre-line;
}

.runtime-about__text--muted {
  color: var(--gcc-muted);
}
</style>
