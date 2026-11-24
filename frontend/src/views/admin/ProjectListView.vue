<script setup lang="ts">
/**
 * 我的项目（路由 /admin/projects）—— PROJECTS 页面。
 *
 * 视觉：xAI 控制台风格（near-black、hairline、mono 标签、pill 按钮）。
 * 功能与 v3 完全一致：加载/错误/空态、页面管理、站点发布、站点设置、编辑信息、删除、访问公开站点。
 * 顶栏用户菜单/主题切换由 AdminShell 提供。
 */
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { listProjects } from '@/api/projects'
import ToastHost from '@/editor/ToastHost.vue'
import { errorMessageOf } from '@/editor/useEditorState'
import { useProjectActions } from '@/composables/useProjectActions'
import PageHeader from '@/components/admin/PageHeader.vue'
import ProjectCard from '@/components/admin/ProjectCard.vue'
import ProjectEditDialog from '@/components/admin/ProjectEditDialog.vue'
import XIcon from '@/components/admin/XIcon.vue'
import type { ProjectView } from '@/types/api'

const route = useRoute()
const router = useRouter()

const projects = ref<ProjectView[]>([])
const loading = ref(true)
const error = ref('')

/* 站点发布 / 删除站点：与总览页共用同一份实现 */
const { busyId, onPublishSite, onDelete } = useProjectActions({ reload: load })

/* 编辑站点信息弹窗 */
const editTarget = ref<ProjectView | null>(null)

function openEditDialog(project: ProjectView): void {
  editTarget.value = project
}

function closeEditDialog(): void {
  editTarget.value = null
}

async function onEditSaved(): Promise<void> {
  editTarget.value = null
  await load()
}

const query = ref(String(route.query.q ?? ''))
const statusFilter = ref<'ALL' | 'PUBLISHED' | 'DRAFT'>('ALL')

const filteredProjects = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return projects.value.filter((project) => {
    if (statusFilter.value !== 'ALL' && project.status !== statusFilter.value) return false
    if (!keyword) return true
    return [project.name, project.slug, project.gameName, project.description]
      .join(' ')
      .toLowerCase()
      .includes(keyword)
  })
})

const publishedCount = computed(
  () => projects.value.filter((project) => project.status === 'PUBLISHED').length,
)
const draftCount = computed(() => projects.value.length - publishedCount.value)

/* ------------------------------ 加载 ------------------------------ */

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    projects.value = await listProjects()
  } catch (err) {
    error.value = errorMessageOf(err, '加载项目失败，请检查后端服务是否启动')
  } finally {
    loading.value = false
  }
}

/* ------------------------------ 导航 ------------------------------ */

function openPages(project: ProjectView): void {
  void router.push({ name: 'page-list', params: { id: project.id } })
}

function openSettings(project: ProjectView): void {
  void router.push({ name: 'site-settings', params: { id: project.id } })
}

function openPublic(project: ProjectView): void {
  window.open(`/community/${encodeURIComponent(project.slug)}`, '_blank', 'noopener')
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="projects-page x-page gcc-container">
    <ToastHost />

    <PageHeader kicker="Projects" title="我的游戏">
      <template #actions>
        <RouterLink class="gcc-btn gcc-btn-sm gcc-btn-ghost" to="/community">
          社区广场
          <XIcon name="arrow-up-right" :size="13" />
        </RouterLink>
        <RouterLink class="gcc-btn gcc-btn-primary" :to="{ name: 'project-create' }">
          <XIcon name="plus" :size="14" />
          新建项目
        </RouterLink>
      </template>
    </PageHeader>

    <div class="projects-toolbar">
      <div class="projects-filters" role="group" aria-label="项目状态筛选">
        <button
          v-for="item in [
            { key: 'ALL', label: '全部' },
            { key: 'PUBLISHED', label: '已发布' },
            { key: 'DRAFT', label: '草稿' },
          ]"
          :key="item.key"
          type="button"
          class="projects-filter"
          :class="{ 'is-active': statusFilter === item.key }"
          @click="statusFilter = item.key as typeof statusFilter"
        >
          {{ item.label }}
        </button>
      </div>
      <span class="projects-toolbar__line" aria-hidden="true"></span>
      <label class="projects-search">
        <XIcon name="search" :size="14" />
        <input
          v-model="query"
          type="search"
          placeholder="按名称 / slug 搜索…"
          aria-label="搜索项目"
        />
      </label>
      <span class="projects-count x-mono">
        {{ filteredProjects.length }} / {{ projects.length }}
      </span>
    </div>

    <!-- 加载中 -->
    <section v-if="loading" class="projects-grid" aria-busy="true">
      <article v-for="index in 4" :key="index" class="project-skeleton">
        <div class="gcc-skeleton" style="height: 148px"></div>
        <div class="project-skeleton__body">
          <div class="gcc-skeleton" style="height: 16px; width: 45%"></div>
          <div class="gcc-skeleton" style="height: 12px; width: 80%"></div>
          <div class="gcc-skeleton" style="height: 12px; width: 60%"></div>
        </div>
      </article>
    </section>

    <!-- 网络错误 -->
    <section v-else-if="error" class="gcc-error projects-error">
      <p>{{ error }}</p>
      <button type="button" class="gcc-btn" @click="load">重试</button>
    </section>

    <!-- 空列表 -->
    <section v-else-if="projects.length === 0" class="projects-empty">
      <p class="x-kicker">NO PROJECTS YET</p>
      <h2 class="x-display-sm">创建你的第一个游戏站点</h2>
      <RouterLink class="gcc-btn gcc-btn-primary" :to="{ name: 'project-create' }">
        <XIcon name="plus" :size="14" />
        创建项目
      </RouterLink>
    </section>

    <!-- 无匹配结果 -->
    <section v-else-if="filteredProjects.length === 0" class="gcc-empty">
      <p>没有匹配的项目</p>
      <button type="button" class="gcc-btn" @click="query = ''; statusFilter = 'ALL'">
        清除筛选
      </button>
    </section>

    <!-- 项目卡片 -->
    <section v-else class="projects-grid">
      <ProjectCard
        v-for="project in filteredProjects"
        :key="project.id"
        :project="project"
        :busy="busyId === project.id"
        class="gcc-fade-in"
        @open="openPages(project)"
        @settings="openSettings(project)"
        @publish="onPublishSite(project)"
        @delete="onDelete(project)"
        @edit="openEditDialog(project)"
        @visit="openPublic(project)"
      />
    </section>

    <footer class="projects-footnote x-mono">
      <span>{{ publishedCount }} PUBLISHED</span>
      <span class="projects-footnote__dot">·</span>
      <span>{{ draftCount }} DRAFTS</span>
      <span class="gcc-spacer"></span>
      <span>DRAFTS ARE PRIVATE UNTIL PUBLISHED</span>
    </footer>

    <!-- 编辑站点信息弹窗（总览页共用同一组件） -->
    <ProjectEditDialog
      v-if="editTarget"
      :project="editTarget"
      @close="closeEditDialog"
      @saved="onEditSaved"
    />
  </div>
</template>

<style scoped>
.projects-page {
  padding-top: 36px;
}

/* ---------------- toolbar ---------------- */
.projects-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 22px;
}

.projects-filters {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 3px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-pill);
  background: var(--x-surface);
}

.projects-filter {
  padding: 5px 13px;
  border: none;
  border-radius: var(--x-radius-pill);
  background: transparent;
  color: var(--x-muted);
  font-size: 12px;
  cursor: pointer;
  transition:
    color 0.15s ease,
    background 0.15s ease;
}

.projects-filter:hover {
  color: var(--x-text);
}

.projects-filter.is-active {
  background: var(--x-surface-3);
  color: var(--x-text);
}

.projects-toolbar__line {
  width: 1px;
  height: 22px;
  background: var(--x-border);
}

.projects-search {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  max-width: 340px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-pill);
  background: var(--x-surface);
  color: var(--x-muted);
}

.projects-search input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  color: var(--x-text);
  font-size: 12px;
}

.projects-search input::placeholder {
  color: var(--x-muted);
}

.projects-count {
  margin-left: auto;
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--x-muted);
}

/* ---------------- grid ---------------- */
.projects-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.project-skeleton {
  overflow: hidden;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
}

.project-skeleton__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
}

.projects-error {
  margin-top: 8px;
}

/* ---------------- empty ---------------- */
.projects-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 80px 24px;
  border: 1px dashed var(--x-border-strong);
  border-radius: var(--x-radius);
  text-align: center;
}

.projects-empty__desc {
  max-width: 520px;
  color: var(--x-muted);
  font-size: 13px;
  line-height: 1.7;
}

.projects-empty h2 {
  margin: 0;
}

/* ---------------- footnote ---------------- */
.projects-footnote {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 36px;
  padding-top: 16px;
  border-top: 1px solid var(--x-border);
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--x-muted);
  opacity: 0.75;
}

.projects-footnote__dot {
  opacity: 0.5;
}

@media (max-width: 1024px) {
  .projects-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .projects-toolbar {
    flex-wrap: wrap;
  }

  .projects-search {
    max-width: none;
    width: 100%;
    order: 5;
  }
}
</style>
