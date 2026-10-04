/**
 * 登录态的事实源（管理端与用户端同源副本）。
 *
 * 为什么不做成 Pinia store：同源的业务组件（builtin:login_form / register_form）两端都要能引用，
 * 而 Pinia 实例要等 app.use(createPinia()) 之后才可用；这里用模块级 reactive + localStorage，
 * 保证「引擎里的 session、localStorage 的 token、页面读到的登录态」只有一份，不出现三处不同步。
 * 用户端的 memberStore 只是它的 Pinia 外观，不另存一份状态。
 */
import { reactive } from "vue";
import http from "./request";

export interface MemberInfo {
    id: number
    username: string
    nickname?: string | null
    avatar?: string | null
    intro?: string | null
}

/** 令牌键沿用 'token'：utils/request.ts 与后端 AuthInterceptor 都认它 */
const TOKEN_KEY = "token";
const MEMBER_KEY = "member_info";

/** 缓存解析失败就当作没有，别让一条坏数据把启动卡死 */
function readCachedMember(): MemberInfo | null {
    try {
        const raw = localStorage.getItem(MEMBER_KEY);
        return raw ? (JSON.parse(raw) as MemberInfo) : null;
    } catch {
        return null;
    }
}

export const sessionState = reactive<{ token: string; member: MemberInfo | null }>({
    token: localStorage.getItem(TOKEN_KEY) ?? "",
    member: readCachedMember(),
});

export function isLoggedIn(): boolean {
    return Boolean(sessionState.token);
}

export function currentMemberId(): number | null {
    return sessionState.member?.id ?? null;
}

/** 落盘令牌与成员信息：登录/注册成功、以及走图登录的 onLoginSuccess 都调它 */
export function saveSession(token: string, member?: MemberInfo | null): void {
    sessionState.token = token;
    localStorage.setItem(TOKEN_KEY, token);
    if (member) {
        sessionState.member = member;
        localStorage.setItem(MEMBER_KEY, JSON.stringify(member));
    }
}

export function clearSession(): void {
    sessionState.token = "";
    sessionState.member = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(MEMBER_KEY);
}

export function logout(): void {
    clearSession();
}

/** 用已存令牌换回最新成员信息；失败（过期/被封）就清掉登录态 */
export async function loadMe(): Promise<MemberInfo | null> {
    if (!sessionState.token) {
        return null;
    }
    try {
        const member = await http.get<MemberInfo, MemberInfo>("/app/member/me");
        sessionState.member = member;
        localStorage.setItem(MEMBER_KEY, JSON.stringify(member));
        return member;
    } catch {
        clearSession();
        return null;
    }
}
