<template>
  <el-container class="layout-container">
    <el-aside width="260px" class="layout-aside">
      <div class="logo">
        <div class="logo-frame">
          <img class="logo-img" src="../assets/zjnu.png" alt="平台标识" />
        </div>
      </div>
      <el-menu :default-active="route.path" router>
        <!-- 系统管理员 -->
        <el-sub-menu v-if="isAdmin" index="admin">
          <template #title><el-icon><Setting /></el-icon><span>系统管理</span></template>
          <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
          <el-menu-item index="/admin/dict"><el-icon><Collection /></el-icon>数据字典</el-menu-item>
        </el-sub-menu>
        <!-- 教务管理员 -->
        <el-sub-menu v-if="isAcademic" index="academic">
          <template #title><el-icon><School /></el-icon><span>教务管理</span></template>
          <el-menu-item index="/academic/courses"><el-icon><Notebook /></el-icon>课程体系</el-menu-item>
          <el-menu-item index="/academic/students"><el-icon><UserFilled /></el-icon>学生管理</el-menu-item>
          <el-menu-item index="/academic/teaching-classes"><el-icon><Tickets /></el-icon>教学班级</el-menu-item>
          <el-menu-item index="/academic/classes"><el-icon><OfficeBuilding /></el-icon>班级管理</el-menu-item>
          <el-menu-item index="/academic/score-unlock"><el-icon><Unlock /></el-icon>成绩解锁</el-menu-item>
          <el-menu-item index="/academic/dashboard"><el-icon><DataBoard /></el-icon>宏观看板</el-menu-item>
          <el-menu-item index="/director/global-compute"><el-icon><TrendCharts /></el-icon>专业级计算</el-menu-item>
        </el-sub-menu>
        <!-- 专业负责人 -->
        <el-sub-menu v-if="isDirector" index="director">
          <template #title><el-icon><DataAnalysis /></el-icon><span>专业管理</span></template>
          <el-menu-item index="/director/grad-req"><el-icon><Document /></el-icon>毕业要求</el-menu-item>
          <el-menu-item index="/director/macro-matrix"><el-icon><Grid /></el-icon>支撑矩阵</el-menu-item>
          <el-menu-item index="/director/dashboard"><el-icon><DataBoard /></el-icon>宏观看板</el-menu-item>
          <el-menu-item index="/director/global-compute"><el-icon><TrendCharts /></el-icon>专业级计算</el-menu-item>
        </el-sub-menu>
        <!-- 主讲教师 -->
        <el-sub-menu v-if="isTeacher" index="teacher">
          <template #title><el-icon><EditPen /></el-icon><span>课程大纲</span></template>
          <div class="teacher-sidebar-selects" @click.stop>
            <div class="teacher-select-label">教学学期</div>
            <el-select v-model="teacherFilterSemester" placeholder="全部学期" size="small" clearable class="teacher-select" @click.stop>
              <el-option v-for="s in teacherSemesters" :key="s.id" :label="s.label" :value="s.id" />
            </el-select>
            <div class="teacher-select-label">教学班级</div>
            <el-select v-model="activeClassId" placeholder="选择教学班级" size="small" class="teacher-select"
              @change="switchTeacherClass" @click.stop>
              <el-option v-for="c in filteredTeacherClasses" :key="c.id"
                :label="`${c.courseName || '课程'} - ${c.className || '班级'+c.id}`" :value="String(c.id)" />
            </el-select>
          </div>
          <el-menu-item :index="`/teacher/${activeClassId}/objectives`"><el-icon><Aim /></el-icon>课程目标</el-menu-item>
          <el-menu-item :index="`/teacher/${activeClassId}/weights`"><el-icon><Histogram /></el-icon>权重分配</el-menu-item>
          <el-menu-item :index="`/teacher/${activeClassId}/assessments`"><el-icon><List /></el-icon>考核点映射</el-menu-item>
          <el-menu-item :index="`/teacher/${activeClassId}/questions`"><el-icon><Edit /></el-icon>题目设置</el-menu-item>
          <el-menu-item :index="`/teacher/${activeClassId}/scores`"><el-icon><Upload /></el-icon>成绩导入</el-menu-item>
          <el-menu-item :index="`/teacher/${activeClassId}/compute`"><el-icon><Finished /></el-icon>课程级计算</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <span class="system-badge">OBE</span>
          <span class="header-title">面向专业认证的毕业要求达成度统一计算平台</span>
        </div>

        <div class="header-right">
          <div class="user-card">
            <span class="user-avatar">{{ userStore.realName?.slice(0, 1) || '用' }}</span>
            <span class="user-info">{{ userStore.realName }}（{{ userStore.roleName }}）</span>
          </div>

          <el-button class="logout-btn" text @click="handleLogout">
            <span class="logout-dot"></span>
            <span>退出登录</span>
          </el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view :key="$route.fullPath" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Setting, User, Collection, School, Notebook, UserFilled, Files, DataAnalysis, Document, Grid, EditPen, Aim, Histogram, List, DataBoard, TrendCharts, Upload, Finished, OfficeBuilding, Tickets, Unlock, Edit } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()


const roleCode = computed(() => userStore.roleCode || localStorage.getItem('roleCode') || '')
const isAdmin = computed(() => roleCode.value === 'ADMIN')
const isAcademic = computed(() => roleCode.value === 'ACADEMIC')
const isDirector = computed(() => roleCode.value === 'DIRECTOR')
const isTeacher = computed(() => roleCode.value === 'TEACHER')
const firstClassId = computed(() => activeClassId.value || '1')
const activeClassId = ref(localStorage.getItem('activeClassId') || '1')
const teacherClasses = ref([])
const teacherSemesters = ref([])
const teacherFilterSemester = ref(null)

const filteredTeacherClasses = computed(() => {
  if (!teacherFilterSemester.value) return teacherClasses.value
  return teacherClasses.value.filter(c => c.semesterId === teacherFilterSemester.value)
})

onMounted(async () => {
  const denied = sessionStorage.getItem('permDenied')
  if (denied) {
    sessionStorage.removeItem('permDenied')
    ElMessage.warning(`无权访问「${denied}」，已跳转到您的首页`)
  }
  if (roleCode.value === 'TEACHER') {
    try {
      const { default: request } = await import('../utils/request')
      const [classRes, semRes] = await Promise.all([
        request.get('/api/teaching-classes/my-classes'),
        request.get('/api/dict/semesters')
      ])
      teacherClasses.value = classRes.data || []
      teacherSemesters.value = semRes.data || []
      if (teacherClasses.value.length > 0 && !teacherClasses.value.find(c => String(c.id) === activeClassId.value)) {
        activeClassId.value = String(teacherClasses.value[0].id)
        localStorage.setItem('activeClassId', activeClassId.value)
      }
    } catch { /* ignore */ }
  }
})

function loadTeacherClasses() {}

function switchTeacherClass(classId) {
  activeClassId.value = classId
  localStorage.setItem('activeClassId', String(classId))
  router.push(`/teacher/${classId}/objectives`)
}

function handleLogout() {
  userStore.logout()
  localStorage.removeItem('roleCode')
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  width: 260px !important;
  flex: 0 0 260px;
  background: #ffffff;
  border-right: 1px solid var(--gray-150);
  overflow-y: auto;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--brand-50) 0%, rgba(246, 244, 250, 0.6) 100%);
  border-bottom: 1px solid var(--brand-100);
}

.logo-frame {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid rgba(158, 137, 205, 0.18);
  box-shadow: 0 8px 18px rgba(128, 107, 191, 0.18);
}

.logo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}

.layout-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.98), rgba(250, 248, 255, 0.96));
  border-bottom: 1px solid rgba(158, 137, 205, 0.14);
  box-shadow: 0 6px 18px rgba(31, 41, 55, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.system-badge {
  height: 28px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.8px;
  color: #ffffff;
  background: linear-gradient(135deg, #9e89cd, #7f67bd);
  box-shadow: 0 8px 18px rgba(128, 107, 191, 0.22);
}

.header-title {
  font-family: "Microsoft YaHei UI", "PingFang SC", "Hiragino Sans GB", sans-serif;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.4px;
  color: transparent;
  background: linear-gradient(90deg, #1e3a8a 0%, #6f55b5 50%, #8c61e8 100%);
  -webkit-background-clip: text;
  background-clip: text;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-card {
  height: 36px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 14px 0 8px;
  border-radius: 999px;
  background: rgba(158, 137, 205, 0.10);
  border: 1px solid rgba(158, 137, 205, 0.18);
  color: #5f4a9c;
}

.user-avatar {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
  color: #ffffff;
  background: linear-gradient(135deg, #9e89cd, #806bbf);
}

.user-info {
  font-size: 14px;
  font-weight: 600;
  color: #5f4a9c;
}

.logout-btn {
  height: 38px;
  padding: 0 18px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
  color: #6c5784;
  background: rgba(247, 240, 240, 0.41);
  border: 1px solid rgba(130, 96, 202, 0.64);
  box-shadow: 0 6px 16px rgba(31, 41, 55, 0.04);
  transition: all 0.2s ease;
}

.logout-btn :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.logout-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f29ca3;
  box-shadow: 0 0 0 4px rgba(242, 156, 163, 0.16);
}

.logout-btn:hover {
  color: #d94f5c;
  background: rgba(229, 107, 111, 0.10);
  border-color: rgba(229, 107, 111, 0.32);
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(229, 107, 111, 0.12);
}

.layout-main {
  flex: 1;
  min-width: 0;
  background: var(--surface-page);
  overflow-y: auto;
}

.teacher-sidebar-selects {
  padding: var(--space-1) var(--space-4);
}

.teacher-select-label {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  margin-bottom: 2px;
}

.teacher-select {
  width: 100%;
  margin-bottom: var(--space-2);
}
</style>
