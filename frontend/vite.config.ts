import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Dev server proxies /api to the Spring Boot backend (port 8080 by default).
// Keep this in sync with docs/API.md (all REST endpoints are under /api).
const apiProxy = {
  '/api': {
    target: process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080',
    changeOrigin: true,
  },
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: apiProxy,
    // 本机文件系统对临时文件 watcher 偶尔报 EBUSY；忽略测试产物目录，避免开发服务器被并行测试/截图脚本打断
    watch: {
      ignored: ['**/node_modules/**', '**/tests/**', '**/.e2e-artifacts/**', '**/dist/**'],
    },
  },
  // `vite preview` 同样反代 /api，便于不装 nginx 时用生产构建做本地验收
  preview: {
    port: 4173,
    proxy: apiProxy,
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
})
