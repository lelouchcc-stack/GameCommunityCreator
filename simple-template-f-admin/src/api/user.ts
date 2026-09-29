import http from "../utils/request";
import type { LoginParams, LoginResult } from "../types/user";

/**
 * 账号密码登录
 *
 * 请求体为 LoginParams，响应经拦截器解包后直接是 Result.data
 */
export function login(params: LoginParams) {
    return http.post<LoginResult, LoginResult>("/admin/login", params);
}
