<script setup lang="ts">
/**
 * 公开端状态块（loading / empty / error / notfound 共用）：
 * mono kicker + 大号标题 + 说明 + 操作插槽。颜色全部走门户主题 --gcc-* 变量。
 */
withDefaults(
  defineProps<{
    variant: 'loading' | 'empty' | 'error' | 'notfound'
    kicker?: string
    title?: string
    description?: string
    retryText?: string
  }>(),
  { kicker: '', title: '', description: '', retryText: '重试' },
)

const emit = defineEmits<{ (event: 'retry'): void }>()
</script>

<template>
  <div class="public-state" :class="`public-state--${variant}`" role="status">
    <p v-if="kicker" class="public-state__kicker">{{ kicker }}</p>

    <template v-if="variant === 'loading'">
      <div class="public-state__skeleton" aria-hidden="true">
        <span class="public-state__line public-state__line--wide"></span>
        <span class="public-state__line public-state__line--mid"></span>
        <span class="public-state__line public-state__line--narrow"></span>
      </div>
      <p class="public-state__desc">{{ title || '正在加载…' }}</p>
    </template>

    <template v-else>
      <h2 v-if="title" class="public-state__title">{{ title }}</h2>
      <p v-if="description" class="public-state__desc">{{ description }}</p>
      <div class="public-state__actions">
        <button
          v-if="variant === 'error'"
          type="button"
          class="public-state__btn"
          @click="emit('retry')"
        >
          {{ retryText }}
        </button>
        <slot />
      </div>
    </template>
  </div>
</template>

<style scoped>
.public-state {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
  padding: 56px 0;
  max-width: 720px;
}

.public-state--error .public-state__title,
.public-state--notfound .public-state__title {
  color: var(--gcc-text);
  font-size: clamp(26px, 3vw, 38px);
  font-weight: 400;
  line-height: 1.12;
  letter-spacing: -0.02em;
}

.public-state__kicker {
  font-family: var(--gcc-mono);
  font-size: 10.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

.public-state__desc {
  color: var(--gcc-muted);
  font-size: 13.5px;
  line-height: 1.8;
}

.public-state__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 4px;
}

.public-state__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 18px;
  border-radius: 9999px;
  border: 1px solid var(--gcc-border-strong);
  background: transparent;
  color: var(--gcc-text);
  font-size: 13px;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.public-state__btn:hover {
  border-color: var(--gcc-text);
  background: var(--gcc-hover);
}

.public-state__skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.public-state__line {
  display: block;
  height: 14px;
  border-radius: 4px;
  background: var(--gcc-hover);
  animation: public-state-pulse 1.6s ease-in-out infinite;
}

.public-state__line--wide {
  width: 86%;
}

.public-state__line--mid {
  width: 64%;
}

.public-state__line--narrow {
  width: 42%;
}

@keyframes public-state-pulse {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 1;
  }
}
</style>
