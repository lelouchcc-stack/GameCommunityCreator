import axios from "axios";

/** 后端统一返回体 com.lelcc.common.Result */
interface Result<T = any> {
    code: number;
    message: string;
    data: T;
}

const http = axios.create({
    baseURL: "/api",
    timeout: 5000,
});

http.interceptors.request.use((config) => {
    const token = localStorage.getItem("token");
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
            if (result.code === 401) {
                // 令牌失效先清掉；用户端登录页与路由守卫落地后再在这里跳转登录
                localStorage.removeItem("token");
            }
            return Promise.reject(new Error(result.message || "请求失败"));
        }
        // 业务成功时只把 Result.data 交给调用方
        return result.data;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default http;
