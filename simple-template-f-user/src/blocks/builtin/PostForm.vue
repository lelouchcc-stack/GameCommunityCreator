<template>
    <section class="post-form">
        <!-- 后台画布：这是"当前登录玩家来发帖"，后台没有玩家登录态可提交。
             这里既不请求 /app/board/**、/app/tag/**（后台令牌会被拒），也不放可点的登录入口
             （会把正在编辑页面的管理员导航走），只交代这个区块的版面长什么样。 -->
        <div v-if="interactive" class="post-form__canvas">
            <h2 v-if="heading" class="post-form__title">{{ heading }}</h2>
            <div class="post-form__fake">
                <div v-for="row in canvasRows" :key="row.label" class="post-form__row">
                    <span class="post-form__label">{{ row.label }}</span>
                    <span class="post-form__fake-control">{{ row.value }}</span>
                </div>
                <div class="post-form__bar">
                    <span class="post-form__submit post-form__submit--fake">{{ submitText }}</span>
                </div>
            </div>
            <p class="ui-state ui-state--inline">
                板块、版本、进度阶段与标签都按板块现取，提交时用的是当前登录玩家；后台画布不代取也不代提交。
            </p>
        </div>

        <p v-else-if="!loggedIn" class="post-form__guest">
            <span class="ui-state">登录后才可以发帖。</span>
            <router-link class="post-form__login" to="/login">去登录</router-link>
        </p>

        <template v-else>
            <h2 v-if="heading" class="post-form__title">{{ heading }}</h2>

            <p v-if="loading" class="ui-state ui-state--inline">发帖表单加载中…</p>
            <p v-else-if="!boards.length" class="ui-state ui-state--inline">
                还没有开放的板块，暂时无法发帖。请在后台「内容管理 → 板块管理」里新增一个板块。
            </p>
            <p v-else-if="!versions.length" class="ui-state ui-state--inline">
                「{{ boardName }}」还没有启用中的游戏版本，暂时无法发帖。帖子要挂在版本下，请先在后台为该板块添加版本。
            </p>

            <form v-else class="post-form__form" @submit.prevent="submit">
                <label class="post-form__row">
                    <span class="post-form__label">所属板块</span>
                    <select v-model.number="form.boardId" class="post-form__control" @change="onBoardChange">
                        <option v-for="board in boards" :key="board.id" :value="board.id">{{ board.name }}</option>
                    </select>
                </label>

                <label class="post-form__row">
                    <span class="post-form__label">
                        游戏版本
                        <span class="post-form__required">必填</span>
                    </span>
                    <select v-model.number="form.versionId" class="post-form__control">
                        <option v-for="version in versions" :key="version.id" :value="version.id">
                            {{ version.versionNo }}{{ version.versionName ? `（${version.versionName}）` : "" }}
                        </option>
                    </select>
                </label>

                <label v-if="showStage && stages.length" class="post-form__row">
                    <span class="post-form__label">进度阶段</span>
                    <select v-model="form.stageId" class="post-form__control">
                        <option :value="null">不指定（通用）</option>
                        <option v-for="stage in stages" :key="stage.id" :value="stage.id">{{ stage.stageName }}</option>
                    </select>
                </label>

                <label class="post-form__row">
                    <span class="post-form__label">标题</span>
                    <input
                        v-model="form.title"
                        class="post-form__control"
                        type="text"
                        maxlength="100"
                        placeholder="一句话说清这篇讲什么，比如「0.2 版本深渊三层配队」"
                    />
                </label>

                <label class="post-form__row">
                    <span class="post-form__label">正文</span>
                    <textarea
                        v-model="form.content"
                        class="post-form__control post-form__control--area"
                        rows="10"
                        maxlength="20000"
                        :placeholder="placeholder"
                    ></textarea>
                </label>

                <div v-if="showCover" class="post-form__row">
                    <span class="post-form__label">封面</span>
                    <div class="post-form__cover">
                        <img v-if="coverUrl" class="post-form__cover-img" :src="coverUrl" alt="封面预览" />
                        <div class="post-form__cover-actions">
                            <label class="post-form__cover-pick">
                                {{ uploading ? "上传中…" : coverUrl ? "更换图片" : "选择图片" }}
                                <input type="file" accept="image/*" :disabled="uploading" @change="onPickCover" />
                            </label>
                            <button v-if="coverUrl" class="post-form__cover-remove" type="button" @click="clearCover">
                                移除
                            </button>
                            <span class="post-form__hint">jpg / png / gif / webp / bmp，5MB 以内；不传也行</span>
                        </div>
                    </div>
                </div>

                <div v-if="showTags && tags.length" class="post-form__row">
                    <span class="post-form__label">
                        分类标签
                        <span class="post-form__hint">最多 5 个</span>
                    </span>
                    <div class="post-form__tags">
                        <button
                            v-for="tag in tags"
                            :key="tag.id"
                            class="post-form__tag"
                            :class="{ 'post-form__tag--on': form.tagIds.includes(tag.id) }"
                            type="button"
                            @click="toggleTag(tag.id)"
                        >
                            {{ tag.tagName }}
                        </button>
                    </div>
                </div>

                <p v-if="errorText" class="post-form__error">{{ errorText }}</p>

                <div class="post-form__bar">
                    <button class="post-form__submit" type="submit" :disabled="submitting || uploading">
                        {{ submitting ? "提交中…" : submitText }}
                    </button>
                    <span class="post-form__hint">提交后进入待审核，管理员通过后其他玩家才能看到</span>
                </div>
            </form>
        </template>
    </section>
</template>

<script lang="ts" setup>
/**
 * 发帖表单（业务组件 builtin:post_form）。
 *
 * 为什么是一个业务组件而不是"表单容器 + 输入框 + 逻辑图"：
 * 发帖要按所选板块联动版本、进度阶段与标签，再上传封面、最后提交——这套联动不是画布上
 * 拖几个节点就能表达的，硬拆成图反而要求每个建站的人都画一遍同样的图。所以这里走
 * 「能自洽的交互放进组件」的老规矩，接口全部是真实调用：
 *   - 板块列表：GET /app/board/list
 *   - 版本/阶段：GET /app/board/{id}
 *   - 标签：GET /app/tag/list?boardId=
 *   - 封面：POST /app/upload/image（MEMBER 令牌，匿名 401）
 *   - 提交：POST /app/post → 落库为待审核
 *
 * 板块默认取「内容属性 boardId > 地址查询串 boardId > 第一个开放板块」，
 * 所以 /publish?boardId=3 能直接预选板块，模板里不用写死任何 id。
 */
import { computed, inject, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import type { UiNode } from "../../types/uiBuilder";
import { getBoardDetail, createPost, type BoardVersionItem, type ProgressStageItem } from "../../api/content";
import { listBoards, type BoardItem } from "../../api/board";
import { listTags, type TagItem } from "../../api/tag";
import { uploadMemberImage } from "../../api/upload";
import { resolveAssetUrl } from "../../utils/asset";
import { isLoggedIn } from "../../utils/session";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";

const props = defineProps<{
    node: UiNode
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());
const route = useRoute();

const content = computed(() => props.node.content ?? {});
const heading = computed(() => String(content.value.title ?? ""));
const placeholder = computed(() => String(content.value.placeholder ?? "写点什么…"));
const submitText = computed(() => String(content.value.submitText ?? "发布"));
const redirectUrl = computed(() => String(content.value.redirectUrl ?? "/profile"));
const showCover = computed(() => content.value.showCover !== false);
const showTags = computed(() => content.value.showTags !== false);
const showStage = computed(() => content.value.showStage !== false);

/** 画布里的假控件：只交代版面，不取数 */
const canvasRows = computed(() => [
    { label: "所属板块", value: "示例游戏" },
    { label: "游戏版本", value: "0.2（当前版本）" },
    ...(showStage.value ? [{ label: "进度阶段", value: "不指定（通用）" }] : []),
    { label: "标题", value: "一句话说清这篇讲什么" },
    { label: "正文", value: "正文内容，支持换行…" },
    ...(showCover.value ? [{ label: "封面", value: "选择图片" }] : []),
    ...(showTags.value ? [{ label: "分类标签", value: "攻略 / 资料 / 讨论" }] : []),
]);

const loggedIn = computed(() => isLoggedIn());

const boards = ref<BoardItem[]>([]);
const versions = ref<BoardVersionItem[]>([]);
const stages = ref<ProgressStageItem[]>([]);
const tags = ref<TagItem[]>([]);
const boardName = ref("");
const loading = ref(false);
const submitting = ref(false);
const uploading = ref(false);
const errorText = ref("");

const form = reactive<{
    boardId?: number
    versionId?: number
    stageId: number | null
    title: string
    content: string
    cover: string
    tagIds: number[]
}>({
    boardId: undefined,
    versionId: undefined,
    stageId: null,
    title: "",
    content: "",
    cover: "",
    tagIds: [],
});

const coverUrl = computed(() => resolveAssetUrl(form.cover));

function num(value: unknown): number | undefined {
    if (value === undefined || value === null || value === "") {
        return undefined;
    }
    const n = Number(value);
    return Number.isFinite(n) ? n : undefined;
}

function failText(error: unknown, fallback: string) {
    return error instanceof Error && error.message ? error.message : fallback;
}

async function loadBoards() {
    loading.value = true;
    try {
        const list = await listBoards();
        boards.value = list;
        const wanted = num(content.value.boardId) ?? num(route.query.boardId);
        // 预选板块：内容属性 > 地址查询串 > 第一个开放板块（板块页带 ?boardId= 过来时就能接上）
        form.boardId = list.some((board) => board.id === wanted) ? wanted : list[0]?.id;
        await loadBoardData();
    } catch (error) {
        boards.value = [];
        errorText.value = failText(error, "板块列表加载失败，请稍后再试");
    } finally {
        loading.value = false;
    }
}

/** 换板块要把版本/阶段/标签整组换掉：跨板块的 versionId 后端会直接拒 */
async function loadBoardData() {
    versions.value = [];
    stages.value = [];
    tags.value = [];
    form.versionId = undefined;
    form.stageId = null;
    form.tagIds = [];
    boardName.value = "";
    const id = form.boardId;
    if (id === undefined) {
        return;
    }
    try {
        const [detail, tagList] = await Promise.all([
            getBoardDetail(id),
            listTags(id).catch(() => [] as TagItem[]),
        ]);
        // 只留启用中的：后端 requireBoardVersion / requireProgressStage 也只认启用的
        versions.value = (detail.listBoardVersion ?? []).filter((item) => item.status === 1);
        stages.value = (detail.listProgressStage ?? []).filter((item) => item.status === 1);
        tags.value = tagList;
        boardName.value = detail.board?.name ?? "";
        const current = versions.value.find((item) => item.isCurrent) ?? versions.value[0];
        form.versionId = current?.id;
    } catch (error) {
        errorText.value = failText(error, "板块信息加载失败，请稍后再试");
    }
}

function onBoardChange() {
    errorText.value = "";
    void loadBoardData();
}

function toggleTag(tagId: number) {
    const index = form.tagIds.indexOf(tagId);
    if (index >= 0) {
        form.tagIds.splice(index, 1);
        return;
    }
    if (form.tagIds.length >= 5) {
        errorText.value = "最多选择 5 个标签";
        return;
    }
    errorText.value = "";
    form.tagIds.push(tagId);
}

async function onPickCover(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    // 立刻清掉选择值：同一个文件改完再选一次也要能触发 change
    input.value = "";
    if (!file) {
        return;
    }
    // 后端上限 5MB，这里先挡一次，省掉白传一遍的时间（服务端仍会复核）
    if (file.size > 5 * 1024 * 1024) {
        errorText.value = "图片大小不能超过 5MB";
        return;
    }
    uploading.value = true;
    errorText.value = "";
    try {
        const result = await uploadMemberImage(file);
        form.cover = result.key;
    } catch (error) {
        errorText.value = failText(error, "图片上传失败，请稍后再试");
    } finally {
        uploading.value = false;
    }
}

function clearCover() {
    form.cover = "";
}

async function submit() {
    if (submitting.value || uploading.value) {
        return;
    }
    errorText.value = "";
    const title = form.title.trim();
    if (form.boardId === undefined) {
        errorText.value = "请选择所属板块";
        return;
    }
    if (form.versionId === undefined) {
        errorText.value = "请选择关联的游戏版本";
        return;
    }
    if (!title) {
        errorText.value = "请输入标题";
        return;
    }
    if (!form.content.trim()) {
        errorText.value = "请输入正文";
        return;
    }

    submitting.value = true;
    try {
        await createPost({
            boardId: form.boardId,
            versionId: form.versionId,
            stageId: form.stageId,
            title,
            content: form.content,
            cover: form.cover || null,
            tagIds: [...form.tagIds],
        });
        runtime.run({
            action: "toast",
            message: "已提交，管理员审核通过后其他玩家就能看到",
            toastType: "success",
        });
        // 提交成功才跳走：留在原页会让玩家以为没提交成功，反复点提交
        runtime.run({ action: "navigate", url: redirectUrl.value });
    } catch (error) {
        errorText.value = failText(error, "提交失败，请稍后再试");
        runtime.run({ action: "toast", message: errorText.value, toastType: "error" });
    } finally {
        submitting.value = false;
    }
}

onMounted(() => {
    // 后台画布不代取：这里的登录态是后台自己的，去调 /app/board/** 只会换来 401
    if (props.interactive) {
        loading.value = false;
        return;
    }
    if (!loggedIn.value) {
        return;
    }
    void loadBoards();
});
</script>

<style scoped>
.post-form {
    width: 100%;
}

.post-form__title {
    margin: 0 0 18px;
    color: var(--w-title-color, var(--site-text-primary));
    font-size: var(--w-title-font-size, 20px);
    font-weight: var(--w-title-font-weight, 700);
}

.post-form__guest {
    display: flex;
    flex-direction: column;
    gap: 12px;
    align-items: center;
}

.post-form__login {
    padding: 8px 22px;
    border-radius: var(--site-radius-pill);
    background-color: var(--site-primary);
    color: #fff;
    font-size: 14px;
}

.post-form__login:hover {
    background-color: var(--site-primary-hover);
    color: #fff;
}

.post-form__form,
.post-form__fake {
    display: flex;
    flex-direction: column;
    gap: var(--w-item-gap, 16px);
}

.post-form__row {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.post-form__label {
    display: flex;
    gap: 6px;
    align-items: center;
    color: var(--w-meta-color, var(--site-text-secondary));
    font-size: var(--w-meta-font-size, 13px);
}

.post-form__required {
    padding: 0 6px;
    border-radius: 3px;
    background-color: var(--site-primary-weak);
    color: var(--site-primary);
    font-size: 12px;
}

.post-form__control {
    width: 100%;
    padding: 9px 12px;
    border: 1px solid var(--site-border);
    border-radius: var(--site-radius-sm);
    background-color: var(--site-bg-card);
    box-sizing: border-box;
    color: var(--w-body-color, var(--site-text-primary));
    font: inherit;
    font-size: 14px;
    outline: none;
}

.post-form__control:focus {
    border-color: var(--site-primary);
}

.post-form__control--area {
    min-height: 180px;
    line-height: 1.7;
    resize: vertical;
}

/* 画布里的假控件：不可点、不可输，只说明版式 */
.post-form__fake-control {
    padding: 9px 12px;
    border: 1px dashed var(--w-item-border-color, var(--site-border));
    border-radius: var(--site-radius-sm);
    color: var(--site-text-muted);
    font-size: 14px;
}

.post-form__cover {
    display: flex;
    gap: 14px;
    align-items: flex-start;
}

.post-form__cover-img {
    flex: none;
    width: 160px;
    height: 100px;
    border: 1px solid var(--site-border);
    border-radius: var(--site-radius-sm);
    object-fit: cover;
}

.post-form__cover-actions {
    display: flex;
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
}

.post-form__cover-pick {
    padding: 7px 16px;
    border: 1px solid var(--site-border);
    border-radius: var(--w-action-radius, var(--site-radius-pill));
    color: var(--w-action-color, var(--site-text-secondary));
    font-size: var(--w-action-font-size, 13px);
    cursor: pointer;
}

.post-form__cover-pick:hover {
    border-color: var(--site-primary);
    color: var(--site-primary);
}

/* 原生 file 控件各浏览器长相不一，统一藏起来，用上面的标签当按钮 */
.post-form__cover-pick input {
    display: none;
}

.post-form__cover-remove {
    padding: 0;
    border: none;
    background: transparent;
    color: var(--site-danger);
    font-size: 13px;
    cursor: pointer;
}

.post-form__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.post-form__tag {
    padding: 5px 14px;
    border: 1px solid var(--site-border);
    border-radius: var(--w-action-radius, var(--site-radius-pill));
    background: transparent;
    color: var(--w-action-color, var(--site-text-secondary));
    font-size: var(--w-action-font-size, 13px);
    cursor: pointer;
}

.post-form__tag:hover {
    border-color: var(--site-primary);
    color: var(--site-primary);
}

.post-form__tag--on {
    border-color: var(--site-primary);
    background-color: var(--site-primary);
    color: #fff;
}

.post-form__tag--on:hover {
    color: #fff;
}

.post-form__error {
    margin: 0;
    color: var(--site-danger);
    font-size: 13px;
}

.post-form__bar {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
}

.post-form__submit {
    padding: 9px 28px;
    border: none;
    border-radius: var(--w-action-radius, var(--site-radius-pill));
    background-color: var(--w-action-background-color, var(--site-primary));
    color: #fff;
    font-size: var(--w-action-font-size, 14px);
    cursor: pointer;
}

.post-form__submit:hover {
    background-color: var(--site-primary-hover);
}

.post-form__submit:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.post-form__submit--fake {
    opacity: 0.7;
    cursor: default;
}

.post-form__hint {
    color: var(--site-text-muted);
    font-size: var(--w-meta-font-size, 12px);
    font-weight: 400;
}

@media (max-width: 640px) {
    .post-form__cover {
        flex-direction: column;
    }
}
</style>
