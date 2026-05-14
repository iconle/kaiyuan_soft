<template>
  <div class="not-found">
    <div class="error-container">
      <div class="error-code">404</div>
      <div class="error-message">页面未找到</div>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/dashboard')
}
</script>

<style scoped>
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}

.error-container {
  text-align: center;
}

.error-code {
  font-size: 120px;
  font-weight: bold;
  color: #e4e7eb;
  margin-bottom: 20px;
}

.error-message {
  font-size: 24px;
  color: #606266;
  margin-bottom: 40px;
}
</style>
