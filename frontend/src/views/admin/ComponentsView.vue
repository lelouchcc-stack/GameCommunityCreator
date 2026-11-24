<script setup lang="ts">
/**
 * 组件库（/admin/components）—— COMPONENTS 页面。
 * 读取运行时组件注册表 COMPONENT_DEFS（与 PAGE_SCHEMA 契约一致），仅展示，不新增业务逻辑。
 */
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { COMPONENT_DEFS } from '@/components/registry'
import PageHeader from '@/components/admin/PageHeader.vue'
import XIcon from '@/components/admin/XIcon.vue'

const components = computed(() => Object.values(COMPONENT_DEFS))

const artLetter: Record<string, string> = {
  hero: 'H',
  about: 'A',
  notice_list: 'N',
  footer: 'F',
  button: 'B',
  changelog_list: 'C',
  docs_list: 'D',
  team_grid: 'T',
  issue_form: 'I',
  links_grid: 'L',
}

function dataSourceOf(type: string): string {
  const map: Record<string, string> = {
    hero: 'game_info',
    about: 'game_info',
    notice_list: 'announcements',
    changelog_list: 'changelogs',
    docs_list: 'docs',
    team_grid: 'team',
    issue_form: '—',
    links_grid: 'links',
    footer: '—',
    button: '—',
  }
  return map[type] ?? '—'
}
</script>

<template>
  <div class="components-page x-page gcc-container">
    <PageHeader kicker="Component library" title="可复用页面组件">
      <template #actions>
        <RouterLink class="gcc-btn gcc-btn-ghost gcc-btn-sm" :to="{ name: 'admin-templates' }">
          查看模板
        </RouterLink>
        <RouterLink class="gcc-btn gcc-btn-primary" :to="{ name: 'project-list' }">
          去编辑页面
          <XIcon name="arrow-right" :size="13" />
        </RouterLink>
      </template>
    </PageHeader>

    <div class="components-summary x-mono">
      <span>{{ String(components.length).padStart(2, '0') }} COMPONENTS</span>
      <span class="components-summary__sep">/</span>
      <span>7 DATA SOURCES</span>
      <span class="components-summary__sep">/</span>
      <span>JSON DRIVEN · MAX 20 / PAGE</span>
    </div>

    <div class="components-grid">
      <article v-for="component in components" :key="component.type" class="component-card">
        <div class="component-card__art" aria-hidden="true">
          <span class="component-card__letter x-mono">{{ artLetter[component.type] ?? '?' }}</span>
          <span class="component-card__type x-mono">{{ component.type }}</span>
        </div>
        <div class="component-card__body">
          <h2 class="component-card__name">{{ component.label }}</h2>
          <p class="component-card__desc">{{ component.description }}</p>
          <div class="component-card__meta">
            <span class="component-card__meta-label x-mono">DATA SOURCE</span>
            <span class="component-card__meta-value x-mono">{{ dataSourceOf(component.type) }}</span>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.components-page {
  padding-top: 36px;
}

.components-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
  font-size: 9.5px;
  letter-spacing: 0.14em;
  color: var(--x-muted);
  text-transform: uppercase;
}

.components-summary__sep {
  opacity: 0.4;
}

.components-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.component-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
  transition: border-color 0.15s ease;
}

.component-card:hover {
  border-color: var(--x-border-strong);
}

.component-card__art {
  position: relative;
  height: 92px;
  border-bottom: 1px solid var(--x-border);
  background:
    linear-gradient(to right, rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    #0c0c0e;
  background-size:
    26px 26px,
    26px 26px,
    auto;
}

.component-card__letter {
  position: absolute;
  left: 14px;
  bottom: 0;
  font-size: 44px;
  line-height: 1;
  color: rgba(255, 255, 255, 0.09);
}

.component-card__type {
  position: absolute;
  top: 12px;
  right: 14px;
  font-size: 8.5px;
  letter-spacing: 0.12em;
  color: var(--x-muted);
  text-transform: lowercase;
}

.component-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
}

.component-card__name {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: -0.01em;
}

.component-card__desc {
  min-height: 54px;
  color: var(--x-muted);
  font-size: 11.5px;
  line-height: 1.65;
}

.component-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid var(--x-border);
}

.component-card__meta-label {
  font-size: 8.5px;
  letter-spacing: 0.14em;
  color: var(--x-muted);
  opacity: 0.7;
}

.component-card__meta-value {
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--x-text-secondary);
}

@media (max-width: 1180px) {
  .components-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .components-grid {
    grid-template-columns: 1fr;
  }
}
</style>
