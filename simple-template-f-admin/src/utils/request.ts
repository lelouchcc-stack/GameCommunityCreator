import axios from "axios";
import { ElMessage } from "element-plus";

import useUserStore from "../store/userStore";

/** 后端统一返回体 com.lelcc.common.Result */
interface Result<T = any> {
    code: number;
    message: string;
    data: T;
}

const http = axios.create({
    baseURL: "/api",
    timeout: 3000,
});

http.interceptors.request.use((config) => {
    const { token } = useUserStore();
    // 后端 AuthInterceptor 从 Authorization 头读取令牌，值必须是 "Bearer xxx"
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

http.interceptors.response.use(
    (response) => {
        const result = response.data as Result;
        if (result.code !== 200) {
            ElMessage.error(result.message || "请求失败");
            if (result.code === 401) {
                // 只对**后台接口**的 401 登出。画布会渲染两端同源的业务组件，
                // 它们可能去调用户端接口（/app/**），那条路径上的 401 的含义是
                // "后台令牌不该调玩家接口"，不是"后台登录态失效"；
                // 不加这个判断就会把正在编辑页面的管理员直接踢到登录页。
                const url = response.config?.url ?? "";
                if (url.startsWith("/admin")) {
                    useUserStore().clear();
                    if (window.location.pathname !== "/login") {
                        window.location.href = "/login";
                    }
                }
            }
            return Promise.reject(new Error(result.message || "请求失败"));
        }
        // 业务成功时只把 Result.data 交给调用方
        return result.data;
    },
    (error) => {
        ElMessage.error(error.response?.data?.message || error.message || "网络异常，请稍后重试");
        return Promise.reject(error);
    }
);

export default http;
