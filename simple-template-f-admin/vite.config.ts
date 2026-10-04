import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:9331',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // 本地存储（provider=local）的图片经后端的 /static/** 暴露，开发环境要一并代理
      '/static': {
        target: 'http://localhost:9331',
        changeOrigin: true,
      },
    },
  },
})
