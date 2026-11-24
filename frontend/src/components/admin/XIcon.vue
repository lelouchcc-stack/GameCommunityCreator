<script setup lang="ts">
/**
 * 极简线性图标集（xAI 风格：1.5px stroke、无填充、几何线条）。
 * 用 name 指定图标，size 控制尺寸（默认 16）。
 */
withDefaults(
  defineProps<{
    name:
      | 'home'
      | 'folder'
      | 'grid'
      | 'box'
      | 'file'
      | 'settings'
      | 'plus'
      | 'arrow-right'
      | 'arrow-up-right'
      | 'search'
      | 'sun'
      | 'moon'
      | 'monitor'
      | 'chevron-down'
      | 'logout'
      | 'menu'
      | 'external'
      | 'edit'
      | 'eye'
      | 'publish'
      | 'dots'
      | 'check'
      | 'close'
      | 'trash'
      | 'copy'
    size?: number
  }>(),
  { size: 16 },
)

const paths: Record<string, string[]> = {
  home: ['M3 10.5 12 3l9 7.5', 'M5.5 9.5V21h13V9.5'],
  folder: ['M3 7.5V19h18V7.5H12l-2-3H3z'],
  grid: ['M4 4h6.5v6.5H4z', 'M13.5 4H20v6.5h-6.5z', 'M4 13.5h6.5V20H4z', 'M13.5 13.5H20V20h-6.5z'],
  box: ['M12 3 4 7.2v9.6L12 21l8-4.2V7.2z', 'M4 7.2 12 11.5l8-4.3', 'M12 11.5V21'],
  file: ['M6 3h8l5 5v13H6z', 'M14 3v5h5'],
  settings: [
    'M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4z',
    'M19.4 13.4a7.6 7.6 0 0 0 0-2.8l2-1.6-2-3.4-2.4 1a7.6 7.6 0 0 0-2.4-1.4L14.2 2H9.8L9.4 4.9a7.6 7.6 0 0 0-2.4 1.4l-2.4-1-2 3.4 2 1.6a7.6 7.6 0 0 0 0 2.8l-2 1.6 2 3.4 2.4-1a7.6 7.6 0 0 0 2.4 1.4l.4 2.9h4.4l.4-2.9a7.6 7.6 0 0 0 2.4-1.4l2.4 1 2-3.4z',
  ],
  plus: ['M12 5v14', 'M5 12h14'],
  'arrow-right': ['M4 12h16', 'm14 6 6 6-6 6'],
  'arrow-up-right': ['M6 18 18 6', 'M8 6h10v10'],
  search: ['M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z', 'm16.5 16.5 4.5 4.5'],
  sun: ['M12 17a5 5 0 1 0 0-10 5 5 0 0 0 0 10z', 'M12 2v2', 'M12 20v2', 'M2 12h2', 'M20 12h2', 'm5 5 1.5 1.5', 'm17.5 17.5 1.5 1.5', 'm19 5-1.5 1.5', 'm6.5 17.5-1.5 1.5'],
  moon: ['M20 14.5A8.5 8.5 0 0 1 9.5 4 8.5 8.5 0 1 0 20 14.5z'],
  monitor: ['M3 4h18v12H3z', 'M8 20h8', 'M12 16v4'],
  'chevron-down': ['m6 9 6 6 6-6'],
  logout: ['M15 4h4v16h-4', 'M11 12H3', 'm7 8-4 4 4 4'],
  menu: ['M4 6h16', 'M4 12h16', 'M4 18h16'],
  external: ['M14 4h6v6', 'M20 4 10 14', 'M20 13v7H4V6h7'],
  edit: ['M4 20h4L20 8l-4-4L4 16z', 'm14 6 4 4'],
  eye: ['M2 12s3.5-6.5 10-6.5S22 12 22 12s-3.5 6.5-10 6.5S2 12 2 12z', 'M12 14.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z'],
  publish: ['M12 20V5', 'm5 11 7-7 7 7', 'M5 21h14'],
  dots: ['M5 12h.01', 'M12 12h.01', 'M19 12h.01'],
  check: ['m4 12 5.5 5.5L20 6'],
  close: ['M5 5l14 14', 'M19 5 5 19'],
  trash: ['M4 7h16', 'M9 7V4h6v3', 'M6 7v14h12V7', 'M10 11v6', 'M14 11v6'],
  copy: ['M8 8h12v12H8z', 'M4 16V4h12'],
}
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.5"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    class="x-icon"
  >
    <path v-for="(d, index) in paths[name]" :key="index" :d="d" />
  </svg>
</template>

<style scoped>
.x-icon {
  display: block;
  flex: none;
}
</style>
