import { createRouter, createWebHistory } from "vue-router"

import login from "../views/login.vue"
import home from "../views/home.vue"
const routes = [
    {path:'/user/login',component:login},
    {path:'/',component:home}
]
const router = createRouter(
    {
        history:createWebHistory(),
        routes,
    }
)
export default router