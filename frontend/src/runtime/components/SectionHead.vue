<script setup lang="ts">
/**
 * 运行时区块标题（各数据组件共用）：mono kicker + 大号标题 + 说明 + 右侧附加插槽。
 *
 * xAI 风格：标题字重 400 / 负字距，技术标签使用 mono 大写宽字距，
 * 颜色全部来自门户主题变量（--gcc-*），不依赖管理端 --x-*。
 */
withDefaults(
  defineProps<{
    title: string
    kicker?: string
    description?: string
  }>(),
  { kicker: '', description: '' },
)
</script>

<template>
  <header class="runtime-section-head">
    <div class="runtime-section-heading">
      <p v-if="kicker" class="runtime-section-kicker">{{ kicker }}</p>
      <h2 class="runtime-section-title">{{ title }}</h2>
      <p v-if="description" class="runtime-section-desc">{{ description }}</p>
    </div>
    <div v-if="$slots.extra" class="runtime-section-extra">
      <slot name="extra"></slot>
    </div>
  </header>
</template>

<style scoped>
.runtime-section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 18px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--gcc-border);
}

.runtime-section-heading {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.runtime-section-kicker {
  font-family: var(--gcc-mono);
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

.runtime-section-title {
  color: var(--gcc-text);
  font-size: clamp(24px, 2.4vw, 32px);
  font-weight: 400;
  line-height: 1.12;
  letter-spacing: -0.02em;
}

.runtime-section-desc {
  color: var(--gcc-muted);
  font-size: 13px;
  line-height: 1.7;
}

.runtime-section-extra {
  flex: none;
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 2px;
}

@media (max-width: 720px) {
  .runtime-section-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
