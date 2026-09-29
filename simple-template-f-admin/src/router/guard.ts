import type { Router } from "vue-router";
import useUserStore from "../store/userStore";

export function setupRouterGuard(router:Router){
    const whiteList = ['/user/login','/user/register'];
    router.beforeEach(async(to)=>{
        const store = useUserStore()
        //未登录
        if(!store.token){
            if(whiteList.includes(to.path)){
                return true;
            }
            return `/user/login?redirect=${encodeURIComponent(to.fullPath)}`
        }
        //已登录
        if(to.path ==='/user/login'){
            return '/'
        }
        return true
        
    })
}