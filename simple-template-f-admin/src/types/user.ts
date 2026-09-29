/** 登录请求体，字段名与后端登录 DTO 保持一致 */
export interface LoginParams {
    username: string;
    password: string;
}

/** 登录用户信息，字段随后端返回的 UserVO 扩展 */
export interface UserInfo {
    id: number;
    username: string;
}

/** 登录成功时 Result.data 的数据结构 */
export interface LoginResult {
    token: string;
    userInfo?: UserInfo;
}
