/**
 * 玩家登录态的事实源（管理端与用户端同源副本）。
 *
 * 为什么不做成 Pinia store：同源的业务组件（builtin:profile / post_actions / comment_list）两端都要能引用，
 * 而 Pinia 实例要等 app.use(createPinia()) 之后才可用；这里用模块级 reactive + localStorage，
 * 保证「引擎里的 session、localStorage 的 token、页面读到的登录态」只有一份，不出现三处不同步。
 * 用户端的 memberStore 只是它的 Pinia 外观，不另存一份状态。
 *
 * ⚠ 令牌必须验明身份再用：两端把令牌存在**同一个键** `localStorage.token` 上
 * （后端令牌里带 userType，ADMIN 与 MEMBER 不可跨端使用）。
 * 后台管理端也引用了本模块（画布要渲染同源的业务组件），如果这里把后台令牌当成玩家登录态，
 * 就会出现"在后台打开帖子详情/个人主页，画布拿着后台令牌去调 /app/member/**，
 * 被后端 401 拒绝、进而把管理员踢出登录"这种越界故障。
 * 所以下面统一用 isMemberToken 过滤：非 MEMBER 令牌一律视作"未登录"，
 * 并且**绝不去删它**——那不是本模块的令牌，删掉就等于把管理员登出。
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

/** 后端签发的身份域，见 com.lelcc.common.UserType */
const MEMBER_TYPE = "MEMBER";

/**
 * 只解码载荷、不验签：前端验签没有意义（密钥在后端），
 * 这里只是判断"这个令牌是不是玩家令牌"，真正的校验由后端做。
 */
function readUserType(token: string): string | null {
    const payload = token.split(".")[1];
    if (!payload) {
        return null;
    }
    try {
        // JWT 用 base64url，先补回标准 base64 的填充字符
        const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
        const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
        const json = JSON.parse(atob(padded)) as { userType?: unknown };
        return json.userType == null ? null : String(json.userType);
    } catch {
        return null;
    }
}

/** 是不是一枚玩家令牌；空串、坏令牌、后台令牌都返回 false */
export function isMemberToken(token: string | null | undefined): boolean {
    return Boolean(token) && readUserType(token as string) === MEMBER_TYPE;
}

/** 缓存解析失败就当作没有，别让一条坏数据把启动卡死 */
function readCachedMember(): MemberInfo | null {
    try {
        const raw = localStorage.getItem(MEMBER_KEY);
        return raw ? (JSON.parse(raw) as MemberInfo) : null;
    } catch {
        return null;
    }
}

const initialToken = localStorage.getItem(TOKEN_KEY) ?? "";
const initialMember = isMemberToken(initialToken) ? readCachedMember() : null;

export const sessionState = reactive<{ token: string; member: MemberInfo | null }>({
    // 只收玩家令牌：后台令牌留在 localStorage 里给后台自己用，不进这份玩家登录态
    token: isMemberToken(initialToken) ? initialToken : "",
    member: initialMember,
});

export function isLoggedIn(): boolean {
    return isMemberToken(sessionState.token);
}

export function currentMemberId(): number | null {
    return isLoggedIn() ? sessionState.member?.id ?? null : null;
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

/**
 * 清掉玩家登录态。
 * 只在当前令牌确实是玩家令牌时才动 localStorage：后台页面里的共享组件也会调它，
 * 不能让它顺手把管理员的令牌删掉。
 */
export function clearSession(): void {
    sessionState.token = "";
    sessionState.member = null;
    if (isMemberToken(localStorage.getItem(TOKEN_KEY))) {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(MEMBER_KEY);
    }
}

export function logout(): void {
    clearSession();
}

/** 用已存令牌换回最新成员信息；失败（过期/被封）就清掉登录态 */
export async function loadMe(): Promise<MemberInfo | null> {
    // 不是玩家令牌就直接返回：既不发这次注定 401 的请求，也不清登录态
    if (!isLoggedIn()) {
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
