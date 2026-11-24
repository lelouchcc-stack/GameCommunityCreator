<script setup lang="ts">
/**
 * 运行时组件的四态渲染器（loading / empty / error / ready）。
 *
 * 由各业务组件复用：只有 ready 时才通过默认插槽渲染真实内容，
 * 这样每个组件不必各写一套加载/空/错误提示，视觉也保持一致。
 * 视觉：门户主题变量（--gcc-*），xAI 风格 mono 标签 + 细边框，无需阴影。
 */
import type { ComponentStatus } from '@/types/runtime'

withDefaults(
  defineProps<{
    status: ComponentStatus
    errorMessage?: string
    retry: () => void
    loadingText?: string
    emptyText?: string
  }>(),
  {
    errorMessage: '',
    loadingText: '正在加载…',
    emptyText: '暂无内容',
  },
)
</script>

<template>
  <div
    v-if="status === 'loading'"
    class="runtime-state runtime-state--loading"
    role="status"
    aria-live="polite"
  >
    <div class="runtime-state__skeleton" aria-hidden="true">
      <span class="runtime-state__line runtime-state__line--wide"></span>
      <span class="runtime-state__line runtime-state__line--mid"></span>
      <span class="runtime-state__line runtime-state__line--narrow"></span>
    </div>
    <span class="runtime-state__text">{{ loadingText }}</span>
  </div>

  <div v-else-if="status === 'empty'" class="runtime-state runtime-state--empty" role="status">
    <span class="runtime-state__kicker">EMPTY</span>
    <span class="runtime-state__text">{{ emptyText }}</span>
  </div>

  <div v-else-if="status === 'error'" class="runtime-state runtime-state--error" role="alert">
    <span class="runtime-state__kicker">ERROR</span>
    <span class="runtime-state__text">{{ errorMessage || '加载失败，请稍后重试' }}</span>
    <button class="runtime-state__retry" type="button" @click="retry">重试</button>
  </div>

  <slot v-else></slot>
</template>

<style scoped>
.runtime-state {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 36px 0;
}

.runtime-state__skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  max-width: 560px;
}

.runtime-state__line {
  display: block;
  height: 12px;
  border-radius: 3px;
  background: var(--gcc-hover);
  animation: runtime-state-pulse 1.6s ease-in-out infinite;
}

.runtime-state__line--wide {
  width: 92%;
}

.runtime-state__line--mid {
  width: 70%;
}

.runtime-state__line--narrow {
  width: 48%;
}

.runtime-state__kicker {
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

.runtime-state__text {
  color: var(--gcc-muted);
  font-size: 13.5px;
  line-height: 1.7;
}

.runtime-state__retry {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 16px;
  border-radius: 9999px;
  border: 1px solid var(--gcc-border-strong);
  background: transparent;
  color: var(--gcc-text);
  font-size: 12.5px;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.runtime-state__retry:hover {
  border-color: var(--gcc-text);
  background: var(--gcc-hover);
}

.runtime-state--error .runtime-state__kicker {
  color: var(--gcc-danger, #f25f5c);
}

@keyframes runtime-state-pulse {
  0%,
  100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}
</style>
