<script setup lang="ts">
/**
 * footer — 页脚（PAGE_SCHEMA §2.5 / BUGFIX_PLAN §1 页脚唯一）。
 *
 * 页脚所有权规则（v3 冻结）：
 * - 页面配置中的 footer 组件是「页面 JSON 驱动路由」的页脚唯一渲染者
 *   （PublicPageShell 检测到可见 footer 组件时会关闭布局页脚）；
 * - 本组件除 copyright / note 外，渲染站点 `navigation` 中 location=footer 的可见条目，
 *   保证「页面配置页脚」与「布局页脚」互斥后，页脚导航不丢失；
 * - 导航条目由 PublicSiteLayout 过滤/解析（站内 pathForPage、外链已校验 http(s)）后 inject 下发。
 *
 * 视觉：极简 hairline + mono 列（品牌字标 / 页脚导航 / 版权）。
 * 固定 dataSource=null，不发任何请求；标题不展示。
 */
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { RuntimeComponentProps } from '@/types/runtime'
import { readTrimmed } from '@/runtime/componentProps'
import { injectFooterNav } from '@/runtime/context'

const props = defineProps<RuntimeComponentProps>()

const footerNav = injectFooterNav()
const copyright = computed(() => readTrimmed(props.component, 'copyright'))
const note = computed(() => readTrimmed(props.component, 'note'))
const showFallbackNote = computed(() => !copyright.value && !note.value)
</script>

<template>
  <footer class="runtime-footer">
    <div class="runtime-footer__inner gcc-container">
      <div class="runtime-footer__top">
        <p class="runtime-footer__brand">GAME COMMUNITY CREATOR</p>
        <nav v-if="footerNav.length" class="runtime-footer__nav" aria-label="页脚导航">
          <template v-for="item in footerNav" :key="item.key">
            <a
              v-if="item.external"
              class="runtime-footer__link"
              :href="item.path"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ item.title }}<span aria-hidden="true">↗</span>
            </a>
            <RouterLink v-else class="runtime-footer__link" :to="item.path">
              {{ item.title }}
            </RouterLink>
          </template>
        </nav>
      </div>

      <div class="runtime-footer__bottom">
        <p v-if="copyright" class="runtime-footer__copyright">{{ copyright }}</p>
        <p v-if="note" class="runtime-footer__note">{{ note }}</p>
        <p v-if="showFallbackNote" class="runtime-footer__note">
          GameCommunityCreator · 可视化社区
        </p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.runtime-footer {
  margin-top: 72px;
  padding: 28px 0 34px;
  border-top: 1px solid var(--gcc-border);
  background: transparent;
}

.runtime-footer__inner {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.runtime-footer__top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px 28px;
}

.runtime-footer__brand {
  font-family: var(--gcc-mono);
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.2em;
  color: var(--gcc-text-secondary, var(--gcc-text));
}

.runtime-footer__nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 22px;
}

.runtime-footer__link {
  font-family: var(--gcc-mono);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  transition: color 0.15s ease;
}

.runtime-footer__link:hover {
  color: var(--gcc-text);
}

.runtime-footer__bottom {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 20px;
  padding-top: 16px;
  border-top: 1px solid var(--gcc-border);
  font-family: var(--gcc-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gcc-muted);
}

.runtime-footer__copyright {
  color: var(--gcc-muted);
}

.runtime-footer__note {
  color: var(--gcc-muted);
  opacity: 0.8;
}
</style>
