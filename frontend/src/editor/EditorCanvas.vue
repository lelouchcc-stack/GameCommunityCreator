<script setup lang="ts">
/**
 * 中栏画布：把页面草稿配置交给 CommunityRuntime 真实渲染（mode="editor"），
 * 外层只负责"编辑器外壳"（浏览器框、宽度切换、主题变量、选中态），不另写渲染器。
 *
 * 站点上下文（projectId/projectSlug/pages/themeMode）由 EditorView 传入，
 * 运行时据此解析按钮动作、应用站点深浅色；editor 模式下点击按钮不会真正跳转。
 *
 * 点击选中：优先读取运行时给每个组件外层元素打的 data-component-id；
 * 若运行时未打该属性，则退化为"左栏页面结构选中"（任务允许的兜底）。
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import CommunityRuntime from '@/runtime/CommunityRuntime.vue'
import { fontFamilyValue, themeToCssVars } from '@/utils/pageConfig'
import type { PageConfig } from '@/types/page'
import type { RuntimeDataLoader } from '@/types/runtime'
import type { SiteRuntimeContext } from '@/types/site'

const props = defineProps<{
  config: PageConfig
  loader: RuntimeDataLoader
  selectedId: string | null
  /** 站点上下文（动作目标解析 / 站点主题模式） */
  site: SiteRuntimeContext
}>()

const emit = defineEmits<{ (event: 'select', id: string | null): void }>()

const widthMode = ref<'desktop' | 'mobile'>('desktop')
const surfaceRef = ref<HTMLElement | null>(null)

const selectedComponent = computed(
  () => props.config.components.find((item) => item.id === props.selectedId) ?? null,
)

const selectedIndex = computed(() =>
  props.config.components.findIndex((item) => item.id === props.selectedId),
)

const surfaceStyle = computed<Record<string, string>>(() => ({
  ...themeToCssVars(props.config.theme),
  'font-family': fontFamilyValue(props.config.theme.fontFamily),
}))

/** 把选中态同步到运行时 DOM（运行时若提供 data-component-id 才会命中） */
function syncSelection(): void {
  const root = surfaceRef.value
  if (!root) return
  root.querySelectorAll<HTMLElement>('[data-component-id]').forEach((element) => {
    element.classList.toggle(
      'gcc-editor-selected',
      element.dataset.componentId === props.selectedId,
    )
  })
}

function onSurfaceClick(event: MouseEvent): void {
  const target = event.target as HTMLElement | null
  if (!target) return
  const wrapper = target.closest<HTMLElement>('[data-component-id]')
  if (!wrapper || !surfaceRef.value?.contains(wrapper)) return
  const id = wrapper.dataset.componentId
  if (id) emit('select', id)
}

watch(
  () => [
    props.selectedId,
    props.config.components.map((item) => item.id).join(','),
    props.config.components.length,
  ],
  () => {
    void nextTick(syncSelection)
  },
)

onMounted(() => {
  void nextTick(syncSelection)
})
</script>

<template>
  <section class="canvas">
    <header class="canvas-toolbar">
      <div class="canvas-heading">
        <span class="canvas-kicker">画布预览</span>
        <strong class="canvas-page-title" :title="config.pageTitle">{{ config.pageTitle }}</strong>
        <span class="gcc-badge gcc-badge-primary">{{ config.components.length }} 个组件</span>
        <span class="canvas-slug gcc-mono">/{{ config.projectSlug }}</span>
      </div>
      <div class="canvas-modes" role="group" aria-label="画布宽度">
        <button
          type="button"
          class="mode-btn"
          :class="{ 'is-active': widthMode === 'desktop' }"
          @click="widthMode = 'desktop'"
        >
          桌面
        </button>
        <button
          type="button"
          class="mode-btn"
          :class="{ 'is-active': widthMode === 'mobile' }"
          @click="widthMode = 'mobile'"
        >
          手机
        </button>
      </div>
    </header>

    <div class="canvas-scroll">
      <div class="canvas-frame" :class="`is-${widthMode}`">
        <div class="canvas-chrome">
          <span class="chrome-dots" aria-hidden="true">
            <i /><i /><i />
          </span>
          <span class="chrome-url gcc-mono">gcc://community/{{ config.projectSlug }}</span>
          <span v-if="selectedComponent" class="chrome-selected">
            已选中：{{ selectedComponent.title }}
          </span>
          <span v-else class="chrome-selected is-muted">未选中组件</span>
        </div>

        <div
          ref="surfaceRef"
          class="canvas-surface"
          :style="surfaceStyle"
          @click="onSurfaceClick"
        >
          <CommunityRuntime :config="config" :loader="loader" mode="editor" :site="site" />
        </div>
      </div>

      <p class="canvas-hint">
        <template v-if="selectedComponent">
          当前选中第 {{ selectedIndex + 1 }} 个组件「{{ selectedComponent.title }}」，可在右侧修改属性。
        </template>
        <template v-else> 点击画布内区块或左侧「页面结构」选中组件；修改即时预览，保存后写入服务器。 </template>
      </p>
    </div>
  </section>
</template>

<style scoped>
.canvas {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.canvas-toolbar {
  flex: none;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 16px;
  border-bottom: 1px solid var(--x-border);
  background: var(--x-canvas-soft);
}

.canvas-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.canvas-kicker {
  flex: none;
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.canvas-page-title {
  font-size: 12.5px;
  font-weight: 500;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.canvas-slug {
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.04em;
  color: var(--x-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.canvas-modes {
  flex: none;
  display: inline-flex;
  padding: 2px;
  gap: 2px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-pill);
  background: var(--x-surface);
}

.mode-btn {
  padding: 4px 12px;
  border: none;
  border-radius: var(--x-radius-pill);
  background: transparent;
  color: var(--x-muted);
  font-size: 11px;
  cursor: pointer;
  transition:
    color 0.12s ease,
    background 0.12s ease;
}

.mode-btn:hover {
  color: var(--x-text);
}

.mode-btn.is-active {
  color: var(--x-text);
  background: var(--x-surface-3);
}

.canvas-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 20px 22px 24px;
  background-image: radial-gradient(var(--x-border) 1px, transparent 1px);
  background-size: 24px 24px;
}

.canvas-frame {
  margin: 0 auto;
  width: 100%;
  max-width: 920px;
  border-radius: 6px;
  border: 1px solid var(--x-border-strong);
  background: var(--x-elevated, var(--x-surface));
  overflow: hidden;
  transition: max-width 0.2s ease;
}

.canvas-frame.is-mobile {
  max-width: 390px;
}

.canvas-chrome {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 32px;
  padding: 0 12px;
  border-bottom: 1px solid var(--x-border);
  background: var(--x-canvas-soft);
}

.chrome-dots {
  flex: none;
  display: inline-flex;
  gap: 5px;
}

.chrome-dots i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--x-border-strong);
}

.chrome-dots i:first-child {
  background: rgba(242, 95, 92, 0.7);
}

.chrome-dots i:nth-child(2) {
  background: rgba(255, 176, 32, 0.7);
}

.chrome-dots i:nth-child(3) {
  background: rgba(61, 220, 151, 0.7);
}

.chrome-url {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.04em;
  color: var(--x-muted);
}

.chrome-selected {
  flex: none;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.04em;
  color: var(--x-accent);
}

.chrome-selected.is-muted {
  color: var(--x-muted);
}

/* 画布内是门户主题：保留页面主题内联变量，不被管理端主题覆盖 */
.canvas-surface {
  min-height: 460px;
  background: var(--gcc-bg);
  color: var(--gcc-text);
  overflow-x: hidden;
}

.canvas-surface :deep([data-component-id]) {
  position: relative;
  cursor: pointer;
  transition: outline-color 0.12s ease;
}

.canvas-surface :deep([data-component-id]:hover) {
  outline: 1px dashed rgba(255, 122, 23, 0.65);
  outline-offset: 2px;
}

.canvas-surface :deep([data-component-id].gcc-editor-selected) {
  outline: 1px solid var(--x-accent);
  outline-offset: 2px;
  box-shadow: 0 0 0 3px rgba(255, 122, 23, 0.16);
}

.canvas-hint {
  margin: 14px auto 0;
  max-width: 920px;
  font-size: 10px;
  line-height: 1.6;
  letter-spacing: 0.03em;
  color: var(--x-muted);
  text-align: center;
  font-family: var(--x-mono);
}
</style>
