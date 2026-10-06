<template>
    <header class="site-header">
        <div class="site-header__inner">
            <router-link class="site-header__brand" to="/" :title="siteName">
                <img
                    v-if="logoUrl"
                    class="site-header__logo"
                    :src="logoUrl"
                    :alt="siteName"
                    @error="logoFailed = true"
                />
                <!-- Logo 缺省 / 加载失败时的兜底：首字徽标，永远不出现碎图或空位 -->
                <span v-else class="site-header__monogram" aria-hidden="true">{{ monogram }}</span>
                <span class="site-header__names">
                    <span class="site-header__site">{{ siteName }}</span>
                    <span v-if="gameName" class="site-header__game">{{ gameName }}</span>
                </span>
            </router-link>

            <nav class="site-header__nav" aria-label="站点导航">
                <router-link
                    v-for="item in navItems"
                    :key="item.url"
                    class="site-header__link"
                    :class="{ 'site-header__link--active': isActive(item.url) }"
                    :to="item.url"
                >
                    {{ item.label }}
                </router-link>
            </nav>

            <form class="site-header__search" role="search" @submit.prevent="onSearch">
                <input
                    v-model="keyword"
                    class="site-header__search-input"
                    type="search"
                    placeholder="搜索帖子…"
                    aria-label="搜索帖子"
                />
            </form>

            <div class="site-header__auth">
                <template v-if="memberStore.loggedIn">
                    <router-link class="site-header__user" to="/profile">
                        <span class="site-header__nickname">{{ displayName }}</span>
                    </router-link>
                    <button class="site-header__btn site-header__btn--ghost" type="button" @click="onLogout">
                        退出
                    </button>
                </template>
                <template v-else>
                    <router-link class="site-header__btn site-header__btn--ghost" to="/login">登录</router-link>
                    <router-link class="site-header__btn" to="/register">注册</router-link>
                </template>
            </div>

            <button
                class="site-header__toggle"
                type="button"
                :aria-expanded="menuOpen"
                aria-label="展开菜单"
                @click="menuOpen = !menuOpen"
            >
                <span class="site-header__toggle-bar"></span>
                <span class="site-header__toggle-bar"></span>
                <span class="site-header__toggle-bar"></span>
            </button>
        </div>

        <!-- 移动端：导航与登录入口收进抽屉，页头本身在窄屏不换行、不溢出 -->
        <div v-if="menuOpen" class="site-header__drawer">
            <router-link
                v-for="item in navItems"
                :key="`m-${item.url}`"
                class="site-header__drawer-link"
                :class="{ 'site-header__drawer-link--active': isActive(item.url) }"
                :to="item.url"
                @click="menuOpen = false"
            >
                {{ item.label }}
            </router-link>
            <form class="site-header__drawer-search" role="search" @submit.prevent="onSearch">
                <input
                    v-model="keyword"
                    class="site-header__search-input"
                    type="search"
                    placeholder="搜索帖子…"
                    aria-label="搜索帖子"
                />
                <button class="site-header__btn" type="submit">搜索</button>
            </form>
            <div class="site-header__drawer-auth">
                <template v-if="memberStore.loggedIn">
                    <router-link class="site-header__btn site-header__btn--ghost" to="/profile" @click="menuOpen = false">
                        个人主页
                    </router-link>
                    <button class="site-header__btn" type="button" @click="onLogout">退出登录</button>
                </template>
                <template v-else>
                    <router-link class="site-header__btn site-header__btn--ghost" to="/login" @click="menuOpen = false">
                        登录
                    </router-link>
                    <router-link class="site-header__btn" to="/register" @click="menuOpen = false">注册</router-link>
                </template>
            </div>
        </div>
    </header>
</template>

<script lang="ts" setup>
/**
 * 站点页头（用户端外壳的一部分，所有页面共用）。
 *
 * 站点名 / 游戏名 / Logo / 导航菜单全部来自站点配置，因此「改站名即可运行」：
 * 模板里不写死任何一个社区名。导航缺配置时回落到内置的一组页面入口，
 * 保证新部署即使没配菜单也有可用的导航。
 */
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import useSiteStore from "../store/siteStore";
import useMemberStore from "../store/memberStore";

const siteStore = useSiteStore();
const memberStore = useMemberStore();
const route = useRoute();
const router = useRouter();

const logoFailed = ref(false);
const menuOpen = ref(false);
const keyword = ref("");

const siteName = computed(() => siteStore.theme?.siteName || "游戏社区");
const gameName = computed(() => siteStore.theme?.gameName || "");
const logoUrl = computed(() => (logoFailed.value ? "" : siteStore.theme?.logo || ""));
/** 兜底徽标取站点名首字；中文取第一个字，英文取首字母大写 */
const monogram = computed(() => siteName.value.trim().charAt(0).toUpperCase() || "G");

const FALLBACK_NAV = [
    { label: "首页", url: "/" },
    { label: "攻略与资料", url: "/guide" },
    { label: "社区板块", url: "/boards" },
    { label: "公告", url: "/notice" },
];

const navItems = computed(() => {
    const configured = siteStore.theme?.navItems ?? [];
    return configured.length ? configured : FALLBACK_NAV;
});

const displayName = computed(
    () => memberStore.member?.nickname || memberStore.member?.username || "我的主页",
);

/** Logo 换了地址就允许再试一次，否则会一直停在兜底徽标 */
watch(
    () => siteStore.theme?.logo,
    () => {
        logoFailed.value = false;
    },
);

/** 换页收起抽屉，否则移动端跳转后菜单还盖着内容 */
watch(
    () => route.fullPath,
    () => {
        menuOpen.value = false;
    },
);

function isActive(url: string) {
    if (url === "/") {
        return route.path === "/";
    }
    return route.path === url || route.path.startsWith(`${url}/`);
}

function onSearch() {
    const text = keyword.value.trim();
    menuOpen.value = false;
    router.push({ path: "/post/list", query: text ? { keyword: text } : undefined });
}

function onLogout() {
    memberStore.logout();
    menuOpen.value = false;
}
</script>

<style scoped>
.site-header {
    position: sticky;
    top: 0;
    z-index: 50;
    background-color: var(--site-header-bg);
    border-bottom: 1px solid var(--site-border);
    backdrop-filter: saturate(180%) blur(12px);
}

.site-header__inner {
    display: flex;
    align-items: center;
    gap: var(--site-space-4);
    width: 100%;
    max-width: var(--site-container);
    height: var(--site-header-height);
    margin: 0 auto;
    padding: 0 var(--site-gutter);
    box-sizing: border-box;
}

.site-header__brand {
    display: flex;
    flex: none;
    align-items: center;
    gap: 10px;
    min-width: 0;
    color: var(--site-text-primary);
}

.site-header__logo {
    display: block;
    width: 32px;
    height: 32px;
    border-radius: var(--site-radius-sm);
    object-fit: contain;
}

.site-header__monogram {
    display: flex;
    flex: none;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: var(--site-radius-sm);
    background: linear-gradient(135deg, var(--site-primary), var(--site-primary-active));
    color: #fff;
    font-size: 16px;
    font-weight: 700;
    line-height: 1;
    user-select: none;
}

.site-header__names {
    display: flex;
    flex-direction: column;
    min-width: 0;
    line-height: 1.2;
}

.site-header__site {
    overflow: hidden;
    font-size: 16px;
    font-weight: 700;
    letter-spacing: 0.2px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.site-header__game {
    overflow: hidden;
    color: var(--site-text-muted);
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.site-header__nav {
    display: flex;
    flex: 1;
    align-items: center;
    gap: 2px;
    min-width: 0;
    overflow: hidden;
}

.site-header__link {
    flex: none;
    padding: 7px 12px;
    border-radius: var(--site-radius-sm);
    color: var(--site-text-secondary);
    font-size: 14px;
    white-space: nowrap;
    transition: color 0.15s, background-color 0.15s;
}

.site-header__link:hover {
    background-color: var(--site-bg-hover);
    color: var(--site-text-primary);
}

.site-header__link--active {
    background-color: var(--site-primary-weak);
    color: var(--site-primary);
    font-weight: 600;
}

.site-header__search {
    flex: none;
    width: 180px;
}

.site-header__search-input {
    width: 100%;
    padding: 7px 12px;
    border: 1px solid var(--site-border);
    border-radius: var(--site-radius-pill);
    background-color: var(--site-bg-secondary);
    box-sizing: border-box;
    color: var(--site-text-primary);
    font: inherit;
    font-size: 13px;
    outline: none;
    transition: border-color 0.15s, background-color 0.15s;
}

.site-header__search-input:focus {
    border-color: var(--site-primary);
    background-color: var(--site-bg-card);
}

.site-header__search-input::placeholder {
    color: var(--site-text-muted);
}

.site-header__auth {
    display: flex;
    flex: none;
    align-items: center;
    gap: var(--site-space-2);
}

.site-header__user {
    display: block;
    max-width: 120px;
    color: var(--site-text-primary);
}

.site-header__nickname {
    display: block;
    overflow: hidden;
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.site-header__btn {
    display: inline-block;
    flex: none;
    padding: 6px 14px;
    border: 1px solid transparent;
    border-radius: var(--site-radius-pill);
    background-color: var(--site-primary);
    color: #fff;
    font: inherit;
    font-size: 13px;
    white-space: nowrap;
    cursor: pointer;
    transition: background-color 0.15s;
}

.site-header__btn:hover {
    background-color: var(--site-primary-hover);
    color: #fff;
}

.site-header__btn--ghost {
    border-color: var(--site-border);
    background-color: transparent;
    color: var(--site-text-secondary);
}

.site-header__btn--ghost:hover {
    border-color: var(--site-primary);
    background-color: var(--site-primary-weak);
    color: var(--site-primary);
}

.site-header__toggle {
    display: none;
    flex: none;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
    width: 36px;
    height: 36px;
    padding: 0 8px;
    border: 1px solid var(--site-border);
    border-radius: var(--site-radius-sm);
    background: transparent;
    cursor: pointer;
}

.site-header__toggle-bar {
    display: block;
    height: 2px;
    border-radius: 1px;
    background-color: var(--site-text-primary);
}

.site-header__drawer {
    display: none;
    flex-direction: column;
    gap: var(--site-space-1);
    padding: var(--site-space-2) var(--site-gutter) var(--site-space-4);
    border-top: 1px solid var(--site-border);
    background-color: var(--site-bg-card);
}

.site-header__drawer-link {
    padding: 10px 8px;
    border-radius: var(--site-radius-sm);
    color: var(--site-text-secondary);
    font-size: 15px;
}

.site-header__drawer-link--active {
    background-color: var(--site-primary-weak);
    color: var(--site-primary);
    font-weight: 600;
}

.site-header__drawer-search {
    display: flex;
    gap: var(--site-space-2);
    margin-top: var(--site-space-2);
}

.site-header__drawer-auth {
    display: flex;
    gap: var(--site-space-2);
    margin-top: var(--site-space-3);
}

.site-header__drawer-auth .site-header__btn {
    flex: 1;
    text-align: center;
}

/* 平板：收掉搜索框，给导航留空间 */
@media (max-width: 1024px) {
    .site-header__search {
        display: none;
    }

    .site-header__link {
        padding: 7px 9px;
        font-size: 13px;
    }
}

/* 移动端：导航与账号入口收进抽屉 */
@media (max-width: 768px) {
    .site-header__nav,
    .site-header__auth {
        display: none;
    }

    .site-header__toggle {
        display: flex;
    }

    .site-header__drawer {
        display: flex;
    }

    .site-header__brand {
        flex: 1;
    }
}
</style>
