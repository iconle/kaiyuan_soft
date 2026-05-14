<template>
  <router-view v-if="isLoginPage" />

  <el-container v-else class="app-container">
    <el-header>
      <div class="header-content">
        <h1>毕业要求达成度统一计算平台</h1>
        <div class="header-right">
          <el-dropdown>
            <span class="el-dropdown-link">
              {{ userStore.userName || '用户' }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <el-container>
      <el-aside width="200px" class="sidebar">
        <el-menu :router="true" :default-active="activeMenu">
          <el-menu-item index="/dashboard" v-if="hasPermission('view_dashboard')">
            <span>仪表盘</span>
          </el-menu-item>

          <el-sub-menu index="admin" v-if="hasPermission('admin')">
            <template #title>系统管理</template>
            <el-menu-item index="/admin/users">用户管理</el-menu-item>
            <el-menu-item index="/admin/dictionary">数据字典</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="data" v-if="hasPermission('data_manage')">
            <template #title>数据管理</template>
            <el-menu-item index="/data/courses">课程管理</el-menu-item>
            <el-menu-item index="/data/requirements">毕业要求</el-menu-item>
            <el-menu-item index="/data/students">学生名单</el-menu-item>
            <el-menu-item index="/data/matrix">支撑矩阵</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="teaching" v-if="hasPermission('teach')">
            <template #title>教学管理</template>
            <el-menu-item index="/teaching/objectives">课程目标</el-menu-item>
            <el-menu-item index="/teaching/assessment">考核点管理</el-menu-item>
            <el-menu-item index="/teaching/scores">成绩录入</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="calc" v-if="hasPermission('calc')">
            <template #title>计算与报表</template>
            <el-menu-item index="/calc/progress">计算进度</el-menu-item>
            <el-menu-item index="/calc/results">达成度结果</el-menu-item>
            <el-menu-item index="/calc/reports">报表生成</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './stores/userStore'
import { ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isLoginPage = computed(() => route.path === '/login')

const activeMenu = computed(() => {
  return router.currentRoute.value.path
})

const hasPermission = (permission) => {
  return userStore.permissions.includes(permission)
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.el-header {
  background-color: #282c34;
  color: white;
  border-bottom: 1px solid #eee;
}

.header-content {
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-content h1 {
  margin: 0;
  font-size: 18px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.sidebar {
  background-color: #f5f7fa;
  border-right: 1px solid #eee;
}

.main-content {
  background-color: #fff;
  padding: 20px;
}

:deep(.el-menu) {
  border-right: none;
}
</style>
