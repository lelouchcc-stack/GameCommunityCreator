<template>
    <div class="community-template">
        <el-card shadow="never">
            <template #header>
                <div class="community-template__header">
                    <span>创建社区</span>
                    <span class="community-template__hint">
                        选择模板 → 填写社区名称与游戏名称 → 创建，即可得到一套完整可用的社区页面
                    </span>
                </div>
            </template>

            <el-steps :active="step" simple class="community-template__steps">
                <el-step title="选择模板" />
                <el-step title="填写信息" />
                <el-step title="创建完成" />
            </el-steps>

            <el-form label-width="120px" class="community-template__form">
                <el-form-item label="社区模板">
                    <el-radio-group v-model="selectedCode" :disabled="applying">
                        <el-radio
                            v-for="item in templates"
                            :key="item.code"
                            :value="item.code"
                            border
                            class="community-template__radio"
                        >
                            <div class="community-template__name">{{ item.nameZh }}</div>
                            <div class="community-template__meta">{{ item.name }} · v{{ item.version }}</div>
                        </el-radio>
                    </el-radio-group>
                    <div v-if="current" class="community-template__desc">{{ current.description }}</div>
                </el-form-item>

                <template v-if="current">
                    <el-divider content-position="left">基本信息</el-divider>

                    <el-form-item
                        v-for="input in current.inputs"
                        :key="input.key"
                        :label="input.label"
                        :required="input.required"
                    >
                        <el-input
                            v-if="input.type === 'text'"
                            v-model="values[input.key]"
                            :maxlength="input.maxLength ?? undefined"
                            show-word-limit
                            :placeholder="input.placeholder ?? ''"
                            :disabled="applying"
                        />
                        <el-input
                            v-else-if="input.type === 'textarea'"
                            v-model="values[input.key]"
                            type="textarea"
                            :rows="3"
                            :maxlength="input.maxLength ?? undefined"
                            show-word-limit
                            :placeholder="input.placeholder ?? ''"
                            :disabled="applying"
                        />
                        <div v-else-if="input.type === 'color'" class="community-template__color">
                            <el-color-picker v-model="values[input.key]" :disabled="applying" />
                            <span class="community-template__color-text">{{ values[input.key] }}</span>
                        </div>
                        <ImageUpload
                            v-else-if="input.type === 'image'"
                            v-model="values[input.key]"
                        />
                        <el-switch
                            v-else-if="input.type === 'boolean'"
                            v-model="values[input.key]"
                            :disabled="applying"
                        />
                        <el-input
                            v-else
                            v-model="values[input.key]"
                            :disabled="applying"
                        />
                        <div v-if="input.help" class="community-template__help">{{ input.help }}</div>
                    </el-form-item>

                    <el-form-item>
                        <el-button type="primary" :loading="applying" @click="handleApply">
                            创建社区
                        </el-button>
                        <el-button :disabled="applying" @click="resetForm">重置</el-button>
                    </el-form-item>
                </template>
            </el-form>
        </el-card>

        <el-card v-if="report" shadow="never" class="community-template__report">
            <template #header>
                <div class="community-template__header">
                    <span>创建结果</span>
                    <span class="community-template__hint">
                        模板：{{ report.name }} · 站点名称：{{ report.theme.siteName }}
                    </span>
                </div>
            </template>

            <el-alert
                v-for="warning in report.warnings"
                :key="warning"
                type="warning"
                :closable="false"
                show-icon
                :title="warning"
                class="community-template__warning"
            />

            <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="站点名称">
                    {{ report.theme.siteName }}
                    <span v-if="report.theme.gameName" class="community-template__muted">
                        （游戏：{{ report.theme.gameName }}）
                    </span>
                </el-descriptions-item>
                <el-descriptions-item label="主题主色">
                    <span class="community-template__swatch" :style="{ backgroundColor: report.theme.primaryColor }" />
                    {{ report.theme.primaryColor }}
                </el-descriptions-item>
                <el-descriptions-item label="导航菜单">
                    <template v-if="report.theme.navItems?.length">
                        <el-tag
                            v-for="nav in report.theme.navItems"
                            :key="nav.url"
                            size="small"
                            class="community-template__tag"
                        >
                            {{ nav.label }} → {{ nav.url }}
                        </el-tag>
                    </template>
                    <span v-else class="community-template__muted">未配置（使用内置导航）</span>
                </el-descriptions-item>
                <el-descriptions-item label="新建页面">
                    <template v-if="report.createdPages.length">
                        <el-tag
                            v-for="page in report.createdPages"
                            :key="page"
                            size="small"
                            type="success"
                            class="community-template__tag"
                        >
                            {{ page }}
                        </el-tag>
                    </template>
                    <span v-else class="community-template__muted">无</span>
                </el-descriptions-item>
                <el-descriptions-item label="按模板重建">
                    <template v-if="report.rebuiltPages.length">
                        <el-tag
                            v-for="page in report.rebuiltPages"
                            :key="page"
                            size="small"
                            type="warning"
                            class="community-template__tag"
                        >
                            {{ page }}
                        </el-tag>
                    </template>
                    <span v-else class="community-template__muted">无</span>
                </el-descriptions-item>
                <el-descriptions-item label="保留原内容">
                    <template v-if="report.keptPages.length">
                        <el-tag
                            v-for="page in report.keptPages"
                            :key="page"
                            size="small"
                            class="community-template__tag"
                        >
                            {{ page }}
                        </el-tag>
                    </template>
                    <span v-else class="community-template__muted">无</span>
                </el-descriptions-item>
                <el-descriptions-item label="组件库">
                    <div v-if="report.createdComponents.length">
                        新增：{{ report.createdComponents.join("、") }}
                    </div>
                    <div v-if="report.updatedComponents.length">
                        更新：{{ report.updatedComponents.join("、") }}
                    </div>
                    <span
                        v-if="!report.createdComponents.length && !report.updatedComponents.length"
                        class="community-template__muted"
                    >
                        无变化
                    </span>
                </el-descriptions-item>
                <el-descriptions-item label="初始内容">
                    <template v-if="report.seeded.length">
                        <div v-for="item in report.seeded" :key="item">{{ item }}</div>
                    </template>
                    <span v-else class="community-template__muted">未写入</span>
                    <div v-for="item in report.skippedSeed" :key="item" class="community-template__muted">
                        跳过：{{ item }}
                    </div>
                </el-descriptions-item>
            </el-descriptions>

            <div class="community-template__preview">
                <el-input v-model="appBaseUrl" class="community-template__preview-input" placeholder="用户端地址" />
                <el-button type="primary" plain @click="openApp">打开用户端</el-button>
                <el-button plain @click="router.push('/site/page-builder')">去页面搭建微调</el-button>
            </div>
        </el-card>
    </div>
</template>

<script lang="ts" setup>
/**
 * 创建社区（模板建站）。
 *
 * 表单字段完全由后端下发的模板 inputs 决定：加一个模板、或给模板加一个输入项，
 * 这个页面不用改一行代码。这也让「零代码配置上线」在管理端自己身上成立。
 */
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import ImageUpload from "../../../components/ImageUpload.vue";
import { applyTemplate, listTemplates } from "../../../api/template";
import useSiteStore from "../../../store/siteStore";
import type { CommunityTemplate, TemplateApplyReport } from "../../../types/template";

const router = useRouter();
const siteStore = useSiteStore();

const templates = ref<CommunityTemplate[]>([]);
const selectedCode = ref("");
const values = reactive<Record<string, any>>({});
const applying = ref(false);
const report = ref<TemplateApplyReport | null>(null);
/** 仅用于本页预览跳转，不做持久化：用户端的部署地址是环境相关的事 */
const appBaseUrl = ref("http://localhost:5173/");

const current = computed(() => templates.value.find((item) => item.code === selectedCode.value) ?? null);
const step = computed(() => (report.value ? 2 : selectedCode.value ? 1 : 0));


function resetForm() {
    for (const key of Object.keys(values)) {
        delete values[key];
    }
    for (const input of current.value?.inputs ?? []) {
        // 布尔项用 manifest 声明的默认值（通常是"要"），文本项留空由后端兜底默认值
        values[input.key] = input.type === "boolean" ? input.defaultValue !== "false" : input.defaultValue ?? "";
    }
    if (values.primaryColor === "" || values.primaryColor === undefined) {
        values.primaryColor = "#5B5FE9";
    }
}

async function loadTemplates() {
    templates.value = await listTemplates();
    if (templates.value.length && !selectedCode.value) {
        selectedCode.value = templates.value[0]?.code ?? "";
    }
}

watch(selectedCode, resetForm);

async function handleApply() {
    const template = current.value;
    if (!template) {
        return;
    }
    const missing = template.inputs.filter((input) => input.required && !String(values[input.key] ?? "").trim());
    if (missing.length) {
        ElMessage.warning(`请先填写：${missing.map((input) => input.label).join("、")}`);
        return;
    }
    const overwrite = values.overwritePages === true;
    try {
        await ElMessageBox.confirm(
            overwrite
                ? "将按模板重建模板自带的页面（首页/攻略/板块/帖子/公告/个人主页），这些页面上已有的手工改动会被覆盖；其它页面不受影响。确定继续？"
                : "只会补建缺失的页面，已有页面原样保留。确定继续？",
            "创建社区",
            { type: "warning", confirmButtonText: "创建", cancelButtonText: "取消" },
        );
    } catch {
        return;
    }

    applying.value = true;
    try {
        report.value = await applyTemplate(template.code, { ...values });
        // 站点名/主色/Logo 都变了，管理端立即应用以便就地看到新主题
        siteStore.apply(report.value.theme);
        resetForm();
        ElMessage.success("社区创建完成，可打开用户端查看");
    } catch {
        // 失败原因由请求拦截器统一提示
    } finally {
        applying.value = false;
    }
}

function openApp() {
    const url = appBaseUrl.value.trim();
    if (!url) {
        ElMessage.warning("请先填写用户端地址");
        return;
    }
    window.open(url, "_blank");
}

onMounted(loadTemplates);
</script>

<style scoped>
.community-template {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.community-template__header {
    display: flex;
    align-items: baseline;
    gap: 12px;
}

.community-template__hint {
    font-size: 12px;
    color: #909399;
}

.community-template__steps {
    margin-bottom: 20px;
}

.community-template__form {
    max-width: 720px;
}

.community-template__radio {
    height: auto;
    margin-bottom: 8px;
    padding: 10px 16px;
}

.community-template__name {
    font-weight: 600;
    line-height: 1.5;
}

.community-template__meta {
    font-size: 12px;
    color: #909399;
}

.community-template__desc {
    width: 100%;
    margin-top: 8px;
    color: #606266;
    font-size: 13px;
    line-height: 1.7;
}

.community-template__help {
    width: 100%;
    margin-top: 4px;
    color: #909399;
    font-size: 12px;
    line-height: 1.6;
}

.community-template__color {
    display: flex;
    align-items: center;
    gap: 12px;
}

.community-template__color-text {
    color: #909399;
    font-family: monospace;
}

.community-template__report {
    max-width: 900px;
}

.community-template__warning {
    margin-bottom: 12px;
}

.community-template__tag {
    margin: 0 6px 6px 0;
}

.community-template__muted {
    color: #909399;
    font-size: 12px;
}

.community-template__swatch {
    display: inline-block;
    width: 12px;
    height: 12px;
    margin-right: 6px;
    border-radius: 3px;
    vertical-align: -1px;
}

.community-template__preview {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    margin-top: 16px;
}

.community-template__preview-input {
    width: 260px;
}
</style>
