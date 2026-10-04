import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import './blocks/ui-blocks.css'
import App from './App.vue'
import router from './router'
import { applySiteTheme } from './utils/theme'
import { readCachedTheme } from './store/siteStore'

// 挂载前先用缓存的站点主题注上变量，避免等接口返回时闪一下默认色
const cachedTheme = readCachedTheme()
if (cachedTheme) {
    applySiteTheme(cachedTheme, { title: true, favicon: true })
}

createApp(App).use(createPinia()).use(router).mount('#app')
