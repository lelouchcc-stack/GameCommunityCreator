<template>
    <div class="home-page">
        <el-card class="home-card">
            <template #header>管理后台</template>
            <p class="home-card__welcome">
                当前登录账号：{{ userStore.userInfo?.username ?? '未知用户' }}
            </p>
            <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
        </el-card>
    </div>
</template>

<script lang="ts" setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import useUserStore from '../store/userStore'

const router = useRouter()
const userStore = useUserStore()

async function handleLogout() {
    userStore.clear()
    ElMessage.success('已退出登录')
    await router.replace('/user/login')
}
</script>

<style scoped>
.home-page {
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    min-height: 100vh;
    padding: 24px;
}

.home-card {
    width: 100%;
    max-width: 480px;
    text-align: center;
}

.home-card__welcome {
    margin: 0 0 16px;
    color: #606266;
}
</style>
