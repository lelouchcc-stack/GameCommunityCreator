<template>
    <div class="home-module">
        <el-card shadow="never">
            <template #header>
                <div class="home-module__header">
                    <span>首页模块</span>
                    <span class="home-module__hint">
                        启用状态与顺序决定用户端首页的渲染结果；参数为 JSON，如 {"limit":5}
                    </span>
                </div>
            </template>

            <el-table v-loading="loading" :data="rows" border>
                <el-table-column label="模块" min-width="200">
                    <template #default="{ row }">
                        <div class="home-module__name">{{ row.moduleName }}</div>
                        <el-tag size="small" type="info">{{ row.moduleType }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="模块参数（JSON）" min-width="240">
                    <template #default="{ row }">
                        <el-input v-model="row.configJson" placeholder="{}" />
                    </template>
                </el-table-column>
                <el-table-column label="排序" width="200" align="center">
                    <template #default="{ $index }">
                        <span class="home-module__sort">{{ $index + 1 }}</span>
                        <el-button link type="primary" :disabled="$index === 0" @click="move($index, -1)">
                            上移
                        </el-button>
                        <el-button
                            link
                            type="primary"
                            :disabled="$index === rows.length - 1"
                            @click="move($index, 1)"
                        >
                            下移
                        </el-button>
                    </template>
                </el-table-column>
                <el-table-column label="启用" width="100" align="center">
                    <template #default="{ row }">
                        <el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" />
                    </template>
                </el-table-column>
            </el-table>

            <div class="home-module__toolbar">
                <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
                <el-button @click="loadList">重置</el-button>
            </div>
        </el-card>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { listHomeModules, saveHomeModules } from "../../../api/site";
import type { HomeModule } from "../../../types/site";

const loading = ref(false);
const saving = ref(false);
const rows = ref<HomeModule[]>([]);

async function loadList() {
    loading.value = true;
    try {
        rows.value = await listHomeModules();
    } finally {
        loading.value = false;
    }
}

/** 上移/下移只调整本地顺序，顺序以数组下标为准，保存时统一下发 sort */
function move(index: number, offset: number) {
    const next = [...rows.value];
    const [moved] = next.splice(index, 1);
    if (!moved) {
        return;
    }
    next.splice(index + offset, 0, moved);
    rows.value = next;
}

function isJson(text: string) {
    try {
        JSON.parse(text);
        return true;
    } catch {
        return false;
    }
}

async function handleSave() {
    for (const row of rows.value) {
        if (row.configJson && !isJson(row.configJson)) {
            ElMessage.error(`模块「${row.moduleName}」的参数不是合法 JSON`);
            return;
        }
    }

    saving.value = true;
    try {
        rows.value = await saveHomeModules(
            rows.value.map((row, index) => ({ ...row, sort: index + 1 })),
        );
        ElMessage.success("保存成功，用户端刷新后按新配置渲染");
    } catch {
        // 失败原因由请求拦截器统一提示
    } finally {
        saving.value = false;
    }
}

onMounted(loadList);
</script>

<style scoped>
.home-module__header {
    display: flex;
    align-items: baseline;
    gap: 12px;
}

.home-module__hint {
    font-size: 12px;
    color: #909399;
}

.home-module__name {
    font-weight: 600;
}

.home-module__sort {
    display: inline-block;
    width: 24px;
    color: #909399;
}

.home-module__toolbar {
    margin-top: 16px;
}
</style>
