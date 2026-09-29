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
                useUserStore().clear();
                if (window.location.pathname !== "/user/login") {
                    window.location.href = "/user/login";
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
