<script setup lang="ts">
/**
 * 运行时轻提示容器：固定底部居中，渲染 toast.ts 的消息队列。
 * 由 CommunityRuntime 根节点渲染（动作执行失败的提示都出现在这里）。
 */
import { toastState, dismissToast } from '../toast'

const state = toastState()
</script>

<template>
  <Teleport to="body">
    <div class="runtime-toast-host" aria-live="polite" aria-atomic="false">
      <TransitionGroup name="runtime-toast">
        <div
          v-for="item in state.items"
          :key="item.id"
          class="runtime-toast"
          :class="`runtime-toast--${item.kind}`"
          role="status"
          @click="dismissToast(item.id)"
        >
          {{ item.message }}
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.runtime-toast-host {
  position: fixed;
  left: 50%;
  bottom: 32px;
  z-index: 4000;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  transform: translateX(-50%);
  pointer-events: none;
  width: max-content;
  max-width: min(560px, calc(100vw - 48px));
}

.runtime-toast {
  pointer-events: auto;
  padding: 10px 18px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.5;
  color: #f7f9ff;
  background: rgba(20, 27, 48, 0.96);
  border: 1px solid rgba(140, 160, 220, 0.32);
  box-shadow: 0 14px 36px rgba(3, 7, 20, 0.45);
  cursor: pointer;
  text-align: center;
}

.runtime-toast--success {
  border-color: rgba(61, 220, 151, 0.55);
  color: #d9ffef;
}

.runtime-toast--error {
  border-color: rgba(242, 95, 92, 0.6);
  color: #ffd9d8;
}

.runtime-toast-enter-active,
.runtime-toast-leave-active {
  transition:
    opacity 0.22s ease,
    transform 0.22s ease;
}

.runtime-toast-enter-from,
.runtime-toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
