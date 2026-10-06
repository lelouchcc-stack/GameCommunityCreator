import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './style.css'
import './blocks/ui-blocks.css'
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
// 显式挂中文语言包：Element Plus 默认是英文，不挂的话弹窗按钮会是 OK / Cancel、
// 分页会显示 10/page——后台界面全是中文，只有这些内置文案是英文会很突兀
app.use(ElementPlus, { locale: zhCn })
setupRouterGuard(router)
app.mount('#app')
