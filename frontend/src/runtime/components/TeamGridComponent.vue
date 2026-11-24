<script setup lang="ts">
/**
 * team_grid — 制作组介绍（PAGE_SCHEMA §2.9）。
 *
 * 数据由 CommunityRuntime 统一加载（team 数据源）。
 * 头像 URL 可选（仅接受 http/https），否则展示姓名首字占位；columns 控制每行卡片数。
 *
 * 视觉：极简制作组名单 —— 角色 mono 标签 + 姓名 + 一句话简介，hairline 卡片。
 */
import { computed } from 'vue'
import type { TeamMemberView } from '@/types/content'
import type { RuntimeComponentProps } from '@/types/runtime'
import {
  asTeamList,
  readBoolean,
  readBoundedNumber,
  readHttpUrl,
  summarize,
} from '@/runtime/componentProps'
import SectionHead from './SectionHead.vue'
import RuntimeState from './RuntimeState.vue'

const props = defineProps<RuntimeComponentProps>()

const columns = computed(() => readBoundedNumber(props.component, 'columns', 3, 1, 4))
const showRole = computed(() => readBoolean(props.component, 'showRole', true))
const items = computed<TeamMemberView[]>(() => asTeamList(props.data))

const gridStyle = computed<Record<string, string>>(() => ({
  gridTemplateColumns: `repeat(${columns.value}, minmax(0, 1fr))`,
}))

function avatarUrl(member: TeamMemberView): string {
  return readHttpUrl(member.avatarUrl)
}

function initialOf(name: string): string {
  const trimmed = String(name ?? '').trim()
  return trimmed ? trimmed.slice(0, 1) : '?'
}

function bioOf(member: TeamMemberView): string {
  return summarize(member.bio, 140)
}
</script>

<template>
  <section class="runtime-team" :aria-label="component.title">
    <div class="gcc-container">
      <SectionHead :title="component.title" kicker="TEAM / THE MAKERS" />

      <RuntimeState
        :status="status"
        :error-message="errorMessage"
        :retry="retry"
        loading-text="正在加载制作组成员…"
        empty-text="暂无制作组成员"
      >
        <ul class="team-grid" :style="gridStyle">
          <li v-for="member in items" :key="member.id" class="team-card">
            <div class="team-card__head">
              <img
                v-if="avatarUrl(member)"
                class="team-card__avatar"
                :src="avatarUrl(member)"
                :alt="member.name"
                loading="lazy"
              />
              <span v-else class="team-card__avatar team-card__avatar--fallback" aria-hidden="true">
                {{ initialOf(member.name) }}
              </span>
              <p v-if="showRole && member.role" class="team-card__role">{{ member.role }}</p>
            </div>
            <h3 class="team-card__name">{{ member.name }}</h3>
            <p v-if="bioOf(member)" class="team-card__bio">{{ bioOf(member) }}</p>
          </li>
        </ul>
      </RuntimeState>
    </div>
  </section>
</template>

<style scoped>
.runtime-team {
  padding: 64px 0 8px;
}

.team-grid {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.team-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 22px;
  border-radius: var(--gcc-card-radius, 8px);
  border: 1px solid var(--gcc-border);
  background: var(--gcc-card-bg);
  transition: border-color 0.15s ease;
}

.team-card:hover {
  border-color: var(--gcc-border-strong);
}

.team-card__head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.team-card__avatar {
  flex: none;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid var(--gcc-border-strong);
}

.team-card__avatar--fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--gcc-surface-2);
  color: var(--gcc-text);
  font-family: var(--gcc-mono);
  font-size: 15px;
}

.team-card__role {
  font-family: var(--gcc-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--gcc-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-card__name {
  color: var(--gcc-text);
  font-size: 17px;
  font-weight: 500;
  line-height: 1.35;
  letter-spacing: -0.01em;
}

.team-card__bio {
  color: var(--gcc-muted);
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
}

@media (max-width: 900px) {
  .team-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
  }
}

@media (max-width: 560px) {
  .team-grid {
    grid-template-columns: minmax(0, 1fr) !important;
  }
}
</style>
