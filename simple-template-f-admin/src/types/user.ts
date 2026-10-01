/** 登录请求体，字段名与后端登录 DTO 保持一致 */
export interface LoginParams {
    username: string;
    password: string;
}

/** 登录用户信息，对应后端 AdminUserInfoVO */
export interface UserInfo {
    id: number;
    username: string;
    nickname?: string | null;
    avatar?: string | null;
    /** 角色标识，如 ["admin"] */
    roles?: string[];
}

/** 登录成功时 Result.data 的数据结构 */
export interface LoginResult {
    token: string;
    userInfo?: UserInfo;
}
