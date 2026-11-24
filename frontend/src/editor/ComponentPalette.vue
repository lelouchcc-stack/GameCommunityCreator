<script setup lang="ts">
/** 左栏①：组件面板。点击某种组件 → 追加到页面末尾并选中。 */
import { computed } from 'vue'
import { COMPONENT_DEF_LIST } from '@/components/registry'
import { MAX_COMPONENTS, type ComponentType } from '@/types/page'

const props = defineProps<{ count: number }>()
const emit = defineEmits<{ (event: 'add', type: ComponentType): void }>()

const full = computed(() => props.count >= MAX_COMPONENTS)
</script>

<template>
  <section class="palette">
    <header class="panel-head">
      <h2 class="panel-title">组件库</h2>
      <span class="panel-count">{{ count }}/{{ MAX_COMPONENTS }}</span>
    </header>

    <div class="palette-list">
      <button
        v-for="def in COMPONENT_DEF_LIST"
        :key="def.type"
        type="button"
        class="palette-item"
        :disabled="full"
        :title="full ? `已达到 ${MAX_COMPONENTS} 个组件上限` : `添加「${def.label}」`"
        @click="emit('add', def.type)"
      >
        <span class="palette-icon" aria-hidden="true">{{ def.icon }}</span>
        <span class="palette-text">
          <span class="palette-label">{{ def.label }}</span>
          <span class="palette-desc">{{ def.description }}</span>
        </span>
        <span class="palette-plus" aria-hidden="true">＋</span>
      </button>
    </div>

    <p v-if="full" class="palette-full">
      已达到 {{ MAX_COMPONENTS }} 个组件上限，如需新增请先删除其他组件。
    </p>
  </section>
</template>

<style scoped>
.palette {
  /* 组件库是「高度可变」区域：最多占左栏 45%，其余空间留给下方「页面结构」。
   * 之前是 flex: none（按内容自然高度撑开，11 项约 630px），在 1280×720 等常见
   * 视口下会把左栏整列占满甚至溢出被父级 overflow: hidden 裁掉，导致
   * ①组件库末尾的组件点不到 ②「页面结构」被压到十几像素。 */
  flex: 0 1 auto;
  /* 关键：覆盖 flex 子项默认的 min-height: auto，否则内容会把区域撑回去、压不下去 */
  min-height: 0;
  max-height: 45%;
  display: flex;
  flex-direction: column;
  padding: 14px 14px 12px;
  border-bottom: 1px solid var(--x-border);
  overflow: hidden;
}

.panel-head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.panel-title {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.panel-count {
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--x-muted);
}

.panel-hint {
  margin-bottom: 12px;
  font-size: 10.5px;
  line-height: 1.55;
  color: var(--x-muted);
}

.palette-list {
  /* 组件超过可视高度时在库内部滚动（列表本身就是滚动容器） */
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.palette-item {
  /* flex 列滚动容器里子项默认会被压缩而不是溢出，显式 none 才会真正产生滚动 */
  flex: none;
  display: flex;
  align-items: flex-start;
  gap: 9px;
  width: 100%;
  padding: 8px 9px;
  border-radius: 6px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--x-text);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.12s ease,
    background 0.12s ease;
}

.palette-item:hover:not(:disabled) {
  border-color: var(--x-border);
  background: var(--x-surface-2);
}

.palette-item:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.palette-icon {
  flex: none;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  border: 1px solid var(--x-border);
  background: var(--x-canvas-soft);
  font-size: 13px;
}

.palette-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.palette-label {
  font-size: 12px;
  font-weight: 500;
}

.palette-desc {
  font-size: 10px;
  line-height: 1.45;
  color: var(--x-muted);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.palette-plus {
  flex: none;
  font-size: 14px;
  line-height: 24px;
  color: var(--x-muted);
  opacity: 0.5;
}

.palette-item:hover:not(:disabled) .palette-plus {
  color: var(--x-accent);
  opacity: 1;
}

.palette-full {
  flex: none;
  margin-top: 10px;
  font-size: 10.5px;
  color: var(--page-warning-fg, var(--x-warning));
}
</style>
