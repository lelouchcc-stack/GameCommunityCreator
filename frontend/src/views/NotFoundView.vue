<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="not-found">
    <div class="code">404</div>
    <h1>页面不存在或社区尚未发布</h1>
    <p class="gcc-muted">
      请检查访问地址是否正确；公开社区页面只有在管理员发布后才可以访问。
    </p>
    <div class="gcc-row">
      <RouterLink class="gcc-btn gcc-btn-primary" to="/admin/projects">返回社区项目管理</RouterLink>
      <RouterLink class="gcc-btn" to="/community">浏览社区广场</RouterLink>
    </div>
  </main>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 40px 24px;
  text-align: center;
}

.code {
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, var(--gcc-primary), var(--gcc-accent));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

h1 {
  font-size: 22px;
}

p {
  max-width: 520px;
}
</style>
