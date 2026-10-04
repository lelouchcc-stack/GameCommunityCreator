/**
 * 用户端登录态 store：utils/session.ts 的 Pinia 外观。
 *
 * 事实源仍是 session.ts（两端同源的业务组件也读它），这里只让页面以 store 的方式响应式消费，
 * 并挂上「启动时校验令牌」这一步。不要在 store 里另存一份 token/member——会出现两份不同步。
 */
import { computed } from "vue";
import { defineStore } from "pinia";
import {
    clearSession,
    currentMemberId,
    isLoggedIn,
    loadMe as loadMeFromSession,
    sessionState,
} from "../utils/session";

const useMemberStore = defineStore("member", () => {
    const token = computed(() => sessionState.token);
    const member = computed(() => sessionState.member);
    const loggedIn = computed(() => isLoggedIn());
    const memberId = computed(() => currentMemberId());

    /** 启动时用已存令牌校验并刷新成员信息；令牌失效/被封会自动清空登录态 */
    async function loadMe() {
        return loadMeFromSession();
    }

    function logout() {
        clearSession();
    }

    return { token, member, loggedIn, memberId, loadMe, logout };
});

export default useMemberStore;
