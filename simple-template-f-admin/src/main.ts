import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router/index.ts'
import { setupRouterGuard } from './router/guard'

const app = createApp(App);
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
setupRouterGuard(router)
app.mount('#app')
