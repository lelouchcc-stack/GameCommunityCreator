<script setup lang="ts">
/** 全屏右下角的轻量提示容器：在页面模板里放一个 <ToastHost /> 即可。 */
import { useToast } from './useToast'

const toast = useToast()

function iconOf(type: 'success' | 'error' | 'info'): string {
  if (type === 'success') return '✓'
  if (type === 'error') return '!'
  return 'i'
}
</script>

<template>
  <Teleport to="body">
    <TransitionGroup name="gcc-toast" tag="div" class="toast-host" aria-live="polite">
      <div
        v-for="item in toast.items"
        :key="item.id"
        class="toast-item"
        :class="`is-${item.type}`"
        role="status"
      >
        <span class="toast-icon" aria-hidden="true">{{ iconOf(item.type) }}</span>
        <span class="toast-text">{{ item.message }}</span>
        <button
          type="button"
          class="toast-close"
          aria-label="关闭提示"
          @click="toast.dismiss(item.id)"
        >
          ×
        </button>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<style scoped>
.toast-host {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: min(400px, calc(100vw - 40px));
  pointer-events: none;
}

.toast-item {
  pointer-events: auto;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 11px 13px;
  border: 1px solid var(--x-border-strong);
  border-radius: var(--x-radius);
  background: var(--x-surface-2);
  color: var(--x-text);
  font-size: 12.5px;
  line-height: 1.55;
}

.toast-item.is-success {
  border-color: rgba(61, 220, 151, 0.45);
}

.toast-item.is-error {
  border-color: rgba(242, 95, 92, 0.5);
}

.toast-item.is-info {
  border-color: var(--x-border-strong);
}

.toast-icon {
  flex: none;
  width: 18px;
  height: 18px;
  margin-top: 1px;
  border: 1px solid var(--x-border-strong);
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--x-mono);
  font-size: 9px;
  font-weight: 600;
  color: var(--x-muted);
  background: transparent;
}

.is-success .toast-icon {
  border-color: rgba(61, 220, 151, 0.5);
  color: var(--x-success);
}

.is-error .toast-icon {
  border-color: rgba(242, 95, 92, 0.55);
  color: var(--x-danger);
}

.is-info .toast-icon {
  border-color: var(--x-border-strong);
  color: var(--x-text-secondary);
}

.toast-text {
  flex: 1;
  overflow-wrap: anywhere;
}

.toast-close {
  flex: none;
  border: none;
  background: transparent;
  color: var(--x-muted);
  font-size: 15px;
  line-height: 1;
  padding: 0 2px;
  cursor: pointer;
}

.toast-close:hover {
  color: var(--x-text);
}

.gcc-toast-enter-active,
.gcc-toast-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.gcc-toast-enter-from,
.gcc-toast-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

/* Toast Teleport 到 body，不在 .admin-theme-scope 内，按 <html data-admin-theme> 适配浅色 */
:global(html[data-admin-theme='light'] .toast-item) {
  background: #ffffff;
  color: #0a0a0a;
  border-color: #c9c9cf;
}

:global(html[data-admin-theme='light'] .toast-close) {
  color: #6b6e75;
}

:global(html[data-admin-theme='light'] .toast-icon) {
  color: #6b6e75;
}
</style>
