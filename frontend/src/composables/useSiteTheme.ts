/**
 * 站点主题组合式函数：公开端主题切换（深色/浅色/跟随系统）+ 本地记忆。
 * 运行时（Agent C）在 PublicSiteLayout 中使用。
 */
import { computed, onBeforeUnmount, onMounted, ref, type ComputedRef, type Ref } from 'vue'
import type { ThemeMode } from '@/types/site'
import {
  nextThemeMode,
  prefersDark,
  readStoredThemeMode,
  resolveThemeMode,
  storeThemeMode,
} from '@/utils/theme'

export interface SiteThemeState {
  /** 访客覆盖的偏好（null = 使用站点默认） */
  userMode: Ref<ThemeMode | null>
  /** 最终生效的明暗模式，用于 data-theme */
  effectiveMode: ComputedRef<'dark' | 'light'>
  /** 站点设置的默认模式 */
  siteMode: ComputedRef<ThemeMode>
  setMode: (mode: ThemeMode | null) => void
  cycleMode: () => void
}

export function useSiteTheme(projectSlug: string, siteMode: () => ThemeMode): SiteThemeState {
  const userMode = ref<ThemeMode | null>(readStoredThemeMode(projectSlug))
  const systemPrefersDark = ref(prefersDark())
  let mediaQuery: MediaQueryList | null = null

  const onChange = (event: MediaQueryListEvent) => {
    systemPrefersDark.value = event.matches
  }

  onMounted(() => {
    if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
      mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      systemPrefersDark.value = mediaQuery.matches
      mediaQuery.addEventListener?.('change', onChange)
    }
  })

  onBeforeUnmount(() => {
    mediaQuery?.removeEventListener?.('change', onChange)
  })

  const currentSiteMode = computed<ThemeMode>(() => siteMode())
  const effectiveMode = computed(() =>
    resolveThemeMode(currentSiteMode.value, userMode.value, systemPrefersDark.value),
  )

  function setMode(mode: ThemeMode | null): void {
    userMode.value = mode
    storeThemeMode(projectSlug, mode)
  }

  function cycleMode(): void {
    setMode(nextThemeMode(userMode.value ?? currentSiteMode.value))
  }

  return { userMode, effectiveMode, siteMode: currentSiteMode, setMode, cycleMode }
}
