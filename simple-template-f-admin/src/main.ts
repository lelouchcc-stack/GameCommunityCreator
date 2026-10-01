import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './style.css'
import App from './App.vue'
import router from './router/index.ts'
import { setupRouterGuard } from './router/guard'

const app = createApp(App);

// 全局注册所有图标：后台菜单 sys_menu.icon 存的就是图标组件名（如 User、Files），靠这里解析
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(name, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
setupRouterGuard(router)
app.mount('#app')
