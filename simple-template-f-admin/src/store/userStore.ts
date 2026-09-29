import { defineStore } from "pinia";
import { ref } from "vue";
import type { UserInfo } from "../types/user";

const TOKEN_KEY = "token";

const useUserStore = defineStore("user", () => {
    const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? "");
    const userInfo = ref<UserInfo | null>(null);

    function setToken(value: string) {
        token.value = value;
        localStorage.setItem(TOKEN_KEY, value);
    }

    function setUserInfo(value: UserInfo | null) {
        userInfo.value = value;
    }

    function clear() {
        token.value = "";
        userInfo.value = null;
        localStorage.removeItem(TOKEN_KEY);
    }

    return { token, userInfo, setToken, setUserInfo, clear };
});

export default useUserStore;
