import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { initAdminTheme } from './composables/useAdminTheme'
import { setUnauthorizedHandler } from './api/client'
import { useAuthStore } from './stores/auth'
import './styles/base.css'
import './styles/markdown.css'

initAdminTheme()

const app = createApp(App)

// 先安装 Pinia：入口、路由守卫、组件里都能直接 useAuthStore()
app.use(createPinia())

const auth = useAuthStore()

// 401（token 过期/未登录）全局处理：清除本地 token 与登录态并跳转登录页
setUnauthorizedHandler(() => {
  auth.clearSession()
  const current = router.currentRoute.value
  if (current.path.startsWith('/admin')) {
    void router.replace({
      name: 'auth-login',
      query: { redirect: current.fullPath, expired: '1' },
    })
  }
})

app.use(router)
app.mount('#app')
