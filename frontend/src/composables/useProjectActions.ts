/**
 * 站点卡片操作（站点发布 / 删除站点）的共享逻辑。
 *
 * 管理端「总览（/admin/overview）」与「我的游戏（/admin/projects）」都会渲染
 * `ProjectCard`，卡片上的「站点发布」「删除站点」必须行为一致（确认文案、
 * busy 态、toast 反馈、列表刷新）。v4.3 修复总览页按钮点击无响应时抽到此处，
 * 避免两个页面各写一份实现再次漂移。
 */
import { ref } from 'vue'
import { publishSite } from '@/api/pages'
import { deleteProject } from '@/api/projects'
import { errorMessageOf } from '@/editor/useEditorState'
import { useToast } from '@/editor/useToast'
import type { ProjectView } from '@/types/api'

export interface ProjectActionsOptions {
  /** 操作成功后重新拉取项目列表（由页面提供，通常是页面的 load()） */
  reload: () => Promise<void> | void
}

export function useProjectActions(options: ProjectActionsOptions) {
  const toast = useToast()
  /** 正在执行操作的站点 ID（用于禁用该卡片按钮，避免重复提交） */
  const busyId = ref<number | null>(null)

  /** 站点发布：发布所有「非停用且草稿版本与发布版本不同」的页面 */
  async function onPublishSite(project: ProjectView): Promise<void> {
    if (busyId.value !== null) return
    const ok = window.confirm(
      `确认发布站点「${project.name}」？\n将发布所有「非停用且草稿版本与发布版本不同」的页面，其余页面跳过。`,
    )
    if (!ok) return
    busyId.value = project.id
    try {
      const result = await publishSite(project.id)
      const published = result.publishedPages.length
      const skipped = result.skippedPages.length
      if (published > 0) {
        toast.success(`站点「${project.name}」发布完成：发布 ${published} 个页面，跳过 ${skipped} 个`)
      } else {
        toast.info(`站点「${project.name}」没有需要发布的页面（跳过 ${skipped} 个）`)
      }
      await options.reload()
    } catch (err) {
      toast.error(`站点发布失败：${errorMessageOf(err, '请稍后重试')}`)
    } finally {
      busyId.value = null
    }
  }

  /** 删除站点：二次确认后级联删除，成功后刷新列表 */
  async function onDelete(project: ProjectView): Promise<void> {
    if (busyId.value !== null) return
    const ok = window.confirm(
      `确定删除站点「${project.name}」吗？\n将级联删除全部页面、草稿/发布快照、导航、设置与内容数据，且无法恢复。`,
    )
    if (!ok) return
    const checkout = window.confirm('删除是不可恢复的操作，请再次确认：真的要删除这个站点吗？')
    if (!checkout) return
    busyId.value = project.id
    try {
      await deleteProject(project.id)
      toast.success(`已删除站点「${project.name}」`)
      await options.reload()
    } catch (err) {
      toast.error(`删除失败：${errorMessageOf(err, '请稍后重试')}`)
    } finally {
      busyId.value = null
    }
  }

  return { busyId, onPublishSite, onDelete }
}
