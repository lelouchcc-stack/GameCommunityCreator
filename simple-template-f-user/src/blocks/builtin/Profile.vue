<template>
    <section class="profile">
        <!-- 后台画布：这是"某个登录玩家自己的主页"，后台没有玩家登录态可展示。
             这里既不请求 /app/member/**（后台令牌会被拒），也不放可点的登录入口
             （会把正在编辑页面的管理员导航走），只交代这个区块的版面长什么样。 -->
        <div v-if="interactive" class="profile__canvas">
            <header class="profile__card">
                <span class="profile__avatar profile__avatar--monogram" aria-hidden="true">玩</span>
                <div class="profile__info">
                    <h2 class="profile__name">玩家昵称</h2>
                    <p class="profile__account">@username</p>
                    <p class="profile__intro profile__intro--empty">还没有填写个人简介</p>
                </div>
                <span class="profile__edit-btn">编辑资料</span>
            </header>
            <p class="ui-state ui-state--inline">
                资料与「我的帖子」取当前登录玩家的数据，后台画布不代取；请在用户端登录后查看真实效果。
            </p>
        </div>

        <p v-else-if="!loggedIn" class="profile__guest">
            <span class="ui-state">登录后可以查看和修改你的个人资料与帖子。</span>
            <router-link class="profile__login-btn" to="/login">去登录</router-link>
        </p>

        <template v-else>
            <header class="profile__card">
                <img
                    v-if="avatarUrl"
                    class="profile__avatar"
                    :src="avatarUrl"
                    :alt="displayName"
                    @error="avatarFailed = true"
                />
                <span v-else class="profile__avatar profile__avatar--monogram" aria-hidden="true">
                    {{ monogram }}
                </span>

                <div class="profile__info">
                    <h2 class="profile__name">{{ displayName }}</h2>
                    <p class="profile__account">@{{ member?.username }}</p>
                    <p v-if="member?.intro" class="profile__intro">{{ member.intro }}</p>
                    <p v-else class="profile__intro profile__intro--empty">还没有填写个人简介</p>
                </div>

                <button class="profile__edit-btn" type="button" @click="toggleEdit">
                    {{ editing ? "取消" : "编辑资料" }}
                </button>
            </header>

            <form v-if="editing" class="profile__form" @submit.prevent="save">
                <div class="profile__field">
                    <span class="profile__label">头像</span>
                    <div class="profile__avatar-row">
                        <img v-if="formAvatarUrl" class="profile__avatar profile__avatar--preview" :src="formAvatarUrl" alt="头像预览" />
                        <span v-else class="profile__avatar profile__avatar--monogram profile__avatar--preview" aria-hidden="true">
                            {{ monogram }}
                        </span>
                        <div class="profile__avatar-actions">
                            <label class="profile__avatar-pick">
                                {{ uploading ? "上传中…" : form.avatar ? "更换头像" : "选择图片" }}
                                <input type="file" accept="image/*" :disabled="uploading" @change="onPickAvatar" />
                            </label>
                            <button
                                v-if="form.avatar"
                                class="profile__avatar-remove"
                                type="button"
                                @click="form.avatar = ''"
                            >
                                移除
                            </button>
                            <span class="profile__hint">jpg / png / gif / webp / bmp，5MB 以内</span>
                        </div>
                    </div>
                </div>
                <label class="profile__field">
                    <span class="profile__label">昵称</span>
                    <input v-model="form.nickname" class="profile__input" maxlength="20" placeholder="显示在帖子与评论上" />
                </label>
                <label class="profile__field">
                    <span class="profile__label">个人简介</span>
                    <textarea
                        v-model="form.intro"
                        class="profile__input profile__input--area"
                        maxlength="200"
                        rows="3"
                        placeholder="介绍一下自己，比如在玩什么、喜欢哪类攻略"
                    ></textarea>
                </label>
                <div class="profile__form-bar">
                    <button class="profile__save" type="submit" :disabled="saving">
                        {{ saving ? "保存中…" : "保存" }}
                    </button>
                </div>
            </form>

            <div class="profile__posts">
                <h3 class="profile__posts-title">
                    我的帖子
                    <span v-if="loaded" class="profile__posts-count">{{ total }}</span>
                </h3>

                <p v-if="loading" class="ui-state ui-state--inline">帖子加载中…</p>
                <p v-else-if="!posts.length" class="ui-state ui-state--inline">{{ emptyText }}</p>

                <ul v-else class="profile__post-list">
                    <li
                        v-for="post in posts"
                        :key="post.id"
                        class="profile__post"
                        :class="{ 'profile__post--link': post.status === 1 }"
                        @click="openPost(post)"
                    >
                        <div class="profile__post-body">
                            <div class="profile__post-title">
                                <span
                                    v-if="post.status !== 1"
                                    class="profile__status"
                                    :class="`profile__status--${post.status}`"
                                >
                                    {{ statusText(post.status) }}
                                </span>
                                {{ post.title }}
                            </div>
                            <div class="profile__post-meta">
                                <span v-if="post.boardName">{{ post.boardName }}</span>
                                <span v-if="post.versionNo">{{ post.versionNo }}</span>
                                <span>赞 {{ post.likeCount }}</span>
                                <span>浏览 {{ post.viewCount }}</span>
                                <span>评论 {{ post.commentCount }}</span>
                            </div>
                            <p v-if="post.status === 2 && post.auditRemark" class="profile__audit">
                                审核意见：{{ post.auditRemark }}
                            </p>
                        </div>
                        <time class="profile__post-date">{{ dateOf(post.createTime) }}</time>
                    </li>
                </ul>

                <div v-if="total > pageSize" class="profile__pager">
                    <button class="profile__page-btn" :disabled="pageNum <= 1" @click="go(-1)">上一页</button>
                    <span class="profile__page-info">{{ pageNum }} / {{ totalPages }}</span>
                    <button class="profile__page-btn" :disabled="pageNum >= totalPages" @click="go(1)">下一页</button>
                </div>
            </div>
        </template>
    </section>
</template>

<script lang="ts" setup>
/**
 * 个人主页（业务组件 builtin:profile）。
 *
 * 三块内容，全部走真实接口：
 *   - 资料卡：GET /app/member/me（App.vue 启动时已校验过令牌）
 *   - 修改资料：PUT /app/member/profile（头像 / 昵称 / 简介；头像走 POST /app/upload/image）
 *   - 我的帖子：GET /app/member/my-posts，含待审核与已下架的，作者自己要看得见
 *
 * 表单与列表都在组件内部完成，逻辑图不需要为这一页画图——
 * 这一页是"业务组件自带行为"的边界示范：能自洽的交互就不要硬拆成图。
 */
import { computed, inject, onMounted, reactive, ref, watch } from "vue";
import type { UiNode } from "../../types/uiBuilder";
import { pageMyPosts, updateProfile } from "../../api/member";
import type { PostListItem } from "../../api/content";
import { uploadMemberImage } from "../../api/upload";
import { resolveAssetUrl } from "../../utils/asset";
import { isLoggedIn, loadMe, sessionState } from "../../utils/session";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";

const props = defineProps<{
    node: UiNode
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());

const content = computed(() => props.node.content ?? {});
const emptyText = computed(() => String(content.value.emptyText ?? "还没有发过帖子"));
const pageSize = computed(() => {
    const n = Number(content.value.pageSize);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : 10;
});

const member = computed(() => sessionState.member);
const loggedIn = computed(() => isLoggedIn());
const displayName = computed(() => member.value?.nickname || member.value?.username || "玩家");

const avatarFailed = ref(false);
const avatarUrl = computed(() => (avatarFailed.value ? "" : resolveAssetUrl(member.value?.avatar)));
const monogram = computed(() => displayName.value.trim().charAt(0).toUpperCase() || "P");

const editing = ref(false);
const saving = ref(false);
const uploading = ref(false);
const form = reactive({ nickname: "", intro: "", avatar: "" });

/** 表单里的头像预览：存的是 key（或历史完整地址），显示时按存储基址现拼 */
const formAvatarUrl = computed(() => resolveAssetUrl(form.avatar));

const posts = ref<PostListItem[]>([]);
const total = ref(0);
const pageNum = ref(1);
const loading = ref(false);
const loaded = ref(false);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));

watch(
    () => sessionState.member?.avatar,
    () => {
        avatarFailed.value = false;
    },
);

function statusText(status?: number) {
    if (status === 0) {
        return "待审核";
    }
    if (status === 2) {
        return "已下架";
    }
    return "";
}

function toggleEdit() {
    editing.value = !editing.value;
    if (editing.value) {
        form.nickname = member.value?.nickname ?? "";
        form.intro = member.value?.intro ?? "";
        // 头像原样带过来：没换就照旧提交，换了就是新上传的 key
        form.avatar = member.value?.avatar ?? "";
    }
}

/** 选图即上传（用户端上传接口 POST /app/upload/image），拿到 key 先预览，点保存才写进资料 */
async function onPickAvatar(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    // 立刻清掉选择值：同一个文件改完再选一次也要能触发 change
    input.value = "";
    if (!file || props.interactive) {
        return;
    }
    // 后端上限 5MB，这里先挡一次，省掉白传一遍的时间（服务端仍会复核）
    if (file.size > 5 * 1024 * 1024) {
        runtime.run({ action: "toast", message: "图片大小不能超过 5MB", toastType: "error" });
        return;
    }
    uploading.value = true;
    try {
        const result = await uploadMemberImage(file);
        form.avatar = result.key;
    } catch (error) {
        runtime.run({
            action: "toast",
            message: error instanceof Error ? error.message : "头像上传失败，请稍后再试",
            toastType: "error",
        });
    } finally {
        uploading.value = false;
    }
}

async function save() {
    if (props.interactive) {
        return;
    }
    saving.value = true;
    try {
        await updateProfile({ nickname: form.nickname, intro: form.intro, avatar: form.avatar });
        // 落盘后回读一次：页面与页头读的是同一份登录态，不会出现"这里改了那里没变"
        await loadMe();
        editing.value = false;
        runtime.run({ action: "toast", message: "资料已更新", toastType: "success" });
    } catch (error) {
        runtime.run({
            action: "toast",
            message: error instanceof Error ? error.message : "保存失败，请稍后再试",
            toastType: "error",
        });
    } finally {
        saving.value = false;
    }
}

async function loadPosts() {
    if (!loggedIn.value) {
        posts.value = [];
        total.value = 0;
        loaded.value = true;
        return;
    }
    loading.value = true;
    try {
        const data = await pageMyPosts(pageNum.value, pageSize.value);
        posts.value = data.list;
        total.value = data.total;
    } catch {
        posts.value = [];
        total.value = 0;
    } finally {
        loading.value = false;
        loaded.value = true;
    }
}

function go(delta: number) {
    const next = pageNum.value + delta;
    if (next < 1 || next > totalPages.value) {
        return;
    }
    pageNum.value = next;
    void loadPosts();
}

/** 只有已发布的帖子才有详情页可看；待审/下架的点了给提示，不做假的可点感 */
function openPost(post: PostListItem) {
    if (props.interactive || post.status !== 1) {
        return;
    }
    runtime.run({ action: "navigate", url: `/post/${post.id}` });
}

function dateOf(value: string) {
    return value ? value.slice(0, 10) : "";
}

onMounted(() => {
    // 后台画布不取玩家数据：这里的登录态是后台自己的，去调 /app/member/** 只会换来 401
    if (props.interactive) {
        loaded.value = true;
        return;
    }
    editing.value = false;
    void loadPosts();
});

watch(() => sessionState.token, loadPosts);
</script>

<style scoped>
.profile {
    width: 100%;
}

/* 后台画布：编辑资料按钮渲染成不可点的标签，避免在画布里触发任何交互 */
.profile__canvas {
    width: 100%;
}

.profile__canvas .profile__edit-btn {
    cursor: default;
}

.profile__guest {
    display: flex;
    flex-direction: column;
    gap: 12px;
    align-items: center;
}

.profile__login-btn {
    padding: 8px 22px;
    border-radius: var(--site-radius-pill);
    background-color: var(--site-primary);
    color: #fff;
    font-size: 14px;
}

.profile__login-btn:hover {
    background-color: var(--site-primary-hover);
    color: #fff;
}

.profile__card {
    display: flex;
    gap: 16px;
    align-items: center;
    padding-bottom: 20px;
    border-bottom: 1px solid var(--w-item-border-color, var(--site-border));
}

.profile__avatar {
    flex: none;
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background-color: var(--site-bg-secondary);
    object-fit: cover;
}

.profile__avatar--monogram {
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, var(--site-primary), var(--site-primary-active));
    color: #fff;
    font-size: 26px;
    font-weight: 700;
    user-select: none;
}

.profile__info {
    flex: 1;
    min-width: 0;
}

.profile__name {
    margin: 0;
    color: var(--w-title-color, var(--site-text-primary));
    font-size: var(--w-title-font-size, 20px);
    font-weight: var(--w-title-font-weight, 700);
    word-break: break-word;
}

.profile__account {
    margin: 2px 0 0;
    color: var(--w-meta-color, var(--site-text-muted));
    font-size: var(--w-meta-font-size, 12px);
}

.profile__intro {
    margin: 8px 0 0;
    color: var(--w-body-color, var(--site-text-secondary));
    font-size: var(--w-body-font-size, 13px);
    line-height: var(--w-body-line-height, 1.7);
    white-space: pre-wrap;
    word-break: break-word;
}

.profile__intro--empty {
    color: var(--site-text-muted);
}

.profile__edit-btn {
    flex: none;
    padding: 6px 16px;
    border: 1px solid var(--site-border);
    border-radius: var(--w-action-radius, var(--site-radius-pill));
    background: transparent;
    color: var(--w-action-color, var(--site-text-secondary));
    font-size: var(--w-action-font-size, 13px);
    cursor: pointer;
}

.profile__edit-btn:hover {
    border-color: var(--site-primary);
    color: var(--site-primary);
}

.profile__form {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-top: 20px;
    padding: 16px;
    border: 1px solid var(--site-border);
    border-radius: var(--site-radius-md);
    background-color: var(--site-bg-secondary);
}

.profile__field {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.profile__label {
    color: var(--site-text-secondary);
    font-size: 13px;
}

.profile__avatar-row {
    display: flex;
    gap: 14px;
    align-items: center;
}

.profile__avatar--preview {
    width: 56px;
    height: 56px;
    font-size: 22px;
}

.profile__avatar-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
}

.profile__avatar-pick {
    padding: 6px 16px;
    border: 1px solid var(--site-border);
    border-radius: var(--w-action-radius, var(--site-radius-pill));
    color: var(--w-action-color, var(--site-text-secondary));
    font-size: var(--w-action-font-size, 13px);
    cursor: pointer;
}

.profile__avatar-pick:hover {
    border-color: var(--site-primary);
    color: var(--site-primary);
}

/* 原生 file 控件各浏览器长相不一，统一藏起来，用上面的标签当按钮 */
.profile__avatar-pick input {
    display: none;
}

.profile__avatar-remove {
    padding: 0;
    border: none;
    background: transparent;
    color: var(--site-danger);
    font-size: 13px;
    cursor: pointer;
}

.profile__hint {
    color: var(--site-text-muted);
    font-size: 12px;
}

.profile__input {
    width: 100%;
    padding: 9px 12px;
    border: 1px solid var(--site-border);
    border-radius: var(--site-radius-sm);
    background-color: var(--site-bg-card);
    box-sizing: border-box;
    color: var(--site-text-primary);
    font: inherit;
    font-size: 14px;
    outline: none;
}

.profile__input:focus {
    border-color: var(--site-primary);
}

.profile__input--area {
    resize: vertical;
    line-height: 1.6;
}

.profile__form-bar {
    display: flex;
    justify-content: flex-end;
}

.profile__save {
    padding: 7px 20px;
    border: none;
    border-radius: var(--site-radius-pill);
    background-color: var(--site-primary);
    color: #fff;
    font-size: 13px;
    cursor: pointer;
}

.profile__save:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.profile__posts {
    margin-top: 24px;
}

.profile__posts-title {
    margin: 0 0 4px;
    color: var(--w-title-color, var(--site-text-primary));
    font-size: var(--w-title-font-size, 16px);
    font-weight: var(--w-title-font-weight, 600);
}

.profile__posts-count {
    margin-left: 6px;
    color: var(--site-text-muted);
    font-size: 13px;
    font-weight: 400;
}

.profile__post-list {
    margin: 0;
    padding: 0;
    list-style: none;
}

.profile__post {
    display: flex;
    gap: 12px;
    align-items: center;
    padding: var(--w-item-padding, 12px 0);
    border-bottom: 1px solid var(--w-item-border-color, var(--site-border));
}

.profile__post--link {
    cursor: pointer;
}

.profile__post--link:hover {
    background-color: var(--w-item-hover-background-color, var(--site-bg-hover));
}

.profile__post-body {
    flex: 1;
    min-width: 0;
}

.profile__post-title {
    overflow: hidden;
    color: var(--w-title-color, var(--site-text-primary));
    font-size: var(--w-title-font-size, 15px);
    font-weight: var(--w-title-font-weight, 500);
    line-height: 1.5;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.profile__status {
    margin-right: 6px;
    padding: 0 6px;
    border-radius: 3px;
    font-size: 12px;
    font-weight: 400;
}

.profile__status--0 {
    background-color: var(--w-badge-background-color, #fff7e6);
    color: var(--w-badge-text-color, var(--site-warning));
}

.profile__status--2 {
    background-color: var(--w-badge-background-color, #fff1f0);
    color: var(--w-badge-text-color, var(--site-danger));
}

.profile__post-meta {
    display: flex;
    flex-wrap: wrap;
    gap: var(--w-meta-gap, 10px);
    margin-top: 4px;
    color: var(--w-meta-color, var(--site-text-muted));
    font-size: var(--w-meta-font-size, 12px);
}

.profile__audit {
    margin: 6px 0 0;
    color: var(--site-danger);
    font-size: 12px;
}

.profile__post-date {
    flex: none;
    color: var(--w-meta-color, var(--site-text-muted));
    font-size: var(--w-meta-font-size, 12px);
}

.profile__pager {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: center;
    margin-top: 16px;
}

.profile__page-btn {
    padding: 4px 12px;
    border: 1px solid var(--site-border);
    border-radius: var(--site-radius-sm);
    background: transparent;
    color: var(--site-primary);
    font-size: 13px;
    cursor: pointer;
}

.profile__page-btn:disabled {
    color: var(--site-text-muted);
    cursor: not-allowed;
}

.profile__page-info {
    color: var(--site-text-muted);
    font-size: 13px;
}

@media (max-width: 640px) {
    .profile__card {
        flex-wrap: wrap;
    }

    .profile__info {
        flex: 1 1 auto;
    }

    .profile__post {
        flex-wrap: wrap;
    }
}
</style>
