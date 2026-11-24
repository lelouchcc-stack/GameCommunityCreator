<script setup lang="ts">
/**
 * 左栏②：页面结构（组件顺序列表）。
 *
 * 支持：点击选中、上移/下移、删除（仅剩 1 个时禁用）、显示/隐藏、HTML5 拖拽排序。
 */
import { ref } from 'vue'
import { getComponentDef } from '@/components/registry'
import type { PageComponent } from '@/types/page'

const props = defineProps<{
  components: PageComponent[]
  selectedId: string | null
}>()

const emit = defineEmits<{
  (event: 'select', id: string): void
  (event: 'move-up', id: string): void
  (event: 'move-down', id: string): void
  (event: 'remove', id: string): void
  (event: 'toggle-visible', id: string): void
  (event: 'reorder', from: number, to: number): void
}>()

const dragFrom = ref<number | null>(null)
const dragOver = ref<number | null>(null)

function defOf(component: PageComponent) {
  return getComponentDef(component.type)
}

function onDragStart(index: number, event: DragEvent): void {
  dragFrom.value = index
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    // Firefox 需要 setData 才会真正开始拖拽
    event.dataTransfer.setData('text/plain', String(index))
  }
}

function onDragOver(index: number): void {
  dragOver.value = index
}

function onDragLeave(index: number): void {
  if (dragOver.value === index) dragOver.value = null
}

function onDrop(index: number): void {
  const from = dragFrom.value
  dragFrom.value = null
  dragOver.value = null
  if (from === null || from === index) return
  emit('reorder', from, index)
}

function onDragEnd(): void {
  dragFrom.value = null
  dragOver.value = null
}

function select(id: string): void {
  emit('select', id)
}
</script>

<template>
  <section class="structure">
    <header class="panel-head">
      <h2 class="panel-title">页面结构</h2>
      <span class="panel-count">{{ components.length }} 个组件</span>
    </header>
    <p class="panel-hint">点击选中；拖动 ⋮⋮ 或使用 ↑↓ 调整顺序。</p>

    <ol class="structure-list">
      <li
        v-for="(component, index) in components"
        :key="component.id"
        class="structure-item"
        :class="{
          'is-selected': component.id === props.selectedId,
          'is-hidden': !component.visible,
          'is-dragging': dragFrom === index,
          'is-drop-target': dragOver === index && dragFrom !== null && dragFrom !== index,
        }"
        draggable="true"
        @dragstart="onDragStart(index, $event)"
        @dragover.prevent="onDragOver(index)"
        @dragleave="onDragLeave(index)"
        @drop.prevent="onDrop(index)"
        @dragend="onDragEnd"
        @click="select(component.id)"
      >
        <div class="item-top">
          <span class="drag-handle" title="拖动排序" aria-hidden="true">⋮⋮</span>
          <span class="item-index">{{ index + 1 }}</span>
          <span class="item-icon" aria-hidden="true">{{ defOf(component).icon }}</span>
          <span class="item-title" :title="component.title">{{ component.title }}</span>
          <button
            type="button"
            class="icon-btn"
            :title="component.visible ? '隐藏该组件（运行时仍保留配置）' : '显示该组件'"
            :aria-label="component.visible ? '隐藏组件' : '显示组件'"
            @click.stop="emit('toggle-visible', component.id)"
          >
            <span v-if="component.visible" aria-hidden="true">👁</span>
            <span v-else aria-hidden="true">🚫</span>
          </button>
        </div>
        <div class="item-bottom">
          <span class="item-type">{{ defOf(component).label }}</span>
          <span v-if="!component.visible" class="hidden-tag">已隐藏</span>
          <span class="gcc-spacer" />
          <button
            type="button"
            class="icon-btn"
            title="上移"
            aria-label="上移"
            :disabled="index === 0"
            @click.stop="emit('move-up', component.id)"
          >
            ↑
          </button>
          <button
            type="button"
            class="icon-btn"
            title="下移"
            aria-label="下移"
            :disabled="index === components.length - 1"
            @click.stop="emit('move-down', component.id)"
          >
            ↓
          </button>
          <button
            type="button"
            class="icon-btn danger"
            title="删除该组件"
            aria-label="删除组件"
            :disabled="components.length <= 1"
            @click.stop="emit('remove', component.id)"
          >
            ✕
          </button>
        </div>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.structure {
  /* 组件库最多占左栏 45%（见 ComponentPalette），这里吃掉剩余高度；
   * flex-basis: 0 让高度只由「剩余空间」决定，不会被自身内容撑大 */
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 14px 14px 0;
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
}

.panel-count {
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--x-muted);
}

.panel-hint {
  flex: none;
  margin-bottom: 10px;
  font-size: 10.5px;
  line-height: 1.55;
  color: var(--x-muted);
}

.structure-list {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 0 0 10px;
  list-style: none;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.structure-item {
  /* 同组件库：flex 列滚动容器里子项需 flex: none 才会溢出滚动而不是被压扁 */
  flex: none;
  position: relative;
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  transition:
    border-color 0.12s ease,
    background 0.12s ease,
    opacity 0.12s ease;
}

.structure-item:hover {
  border-color: var(--x-border);
  background: var(--x-surface-2);
}

.structure-item.is-selected {
  border-color: var(--x-border-strong);
  background: var(--x-surface-3);
  box-shadow: inset 2px 0 0 var(--x-accent);
}

.structure-item.is-hidden {
  opacity: 0.55;
}

.structure-item.is-dragging {
  opacity: 0.35;
}

.structure-item.is-drop-target {
  border-color: rgba(255, 122, 23, 0.5);
  box-shadow: inset 0 2px 0 var(--x-accent);
}

.item-top {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.drag-handle {
  flex: none;
  cursor: grab;
  color: var(--x-muted);
  font-size: 11px;
  letter-spacing: -2px;
  user-select: none;
  opacity: 0.6;
}

.drag-handle:active {
  cursor: grabbing;
}

.item-index {
  flex: none;
  width: 16px;
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.04em;
  color: var(--x-muted);
  text-align: center;
}

.item-icon {
  flex: none;
  font-size: 12px;
}

.item-title {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-bottom {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 5px;
  padding-left: 24px;
}

.item-type {
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
  color: var(--x-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hidden-tag {
  flex: none;
  padding: 0 6px;
  border-radius: var(--x-radius-pill);
  border: 1px solid rgba(255, 176, 32, 0.35);
  font-family: var(--x-mono);
  font-size: 8.5px;
  letter-spacing: 0.06em;
  color: var(--page-warning-fg, var(--x-warning));
}

.icon-btn {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border-radius: 50%;
  border: 1px solid transparent;
  background: transparent;
  color: var(--x-muted);
  font-size: 11px;
  line-height: 1;
  cursor: pointer;
  transition:
    color 0.12s ease,
    background 0.12s ease,
    border-color 0.12s ease;
}

.icon-btn:hover:not(:disabled) {
  color: var(--x-text);
  border-color: var(--x-border);
  background: var(--x-surface-3);
}

.icon-btn.danger:hover:not(:disabled) {
  color: var(--page-danger-fg, var(--x-danger));
  border-color: rgba(242, 95, 92, 0.4);
}

.icon-btn:disabled {
  opacity: 0.28;
  cursor: not-allowed;
}
</style>
