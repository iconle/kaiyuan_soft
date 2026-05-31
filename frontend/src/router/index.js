import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('../layouts/AdminLayout.vue'),
    children: [
      // 系统管理员
      {
        path: 'admin/users',
        name: 'UserManage',
        component: () => import('../views/admin/UserManage.vue'),
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/dict',
        name: 'DictManage',
        component: () => import('../views/admin/DictManage.vue'),
        meta: { roles: ['ADMIN'] }
      },
      // 教务管理员
      {
        path: 'academic/courses',
        name: 'CourseImport',
        component: () => import('../views/academic/CourseImport.vue'),
        meta: { roles: ['ACADEMIC'] }
      },
      {
        path: 'academic/students',
        name: 'AcademicStudentManage',
        component: () => import('../views/admin/StudentManage.vue'),
        meta: { roles: ['ACADEMIC'] }
      },
      {
        path: 'academic/teaching-classes',
        name: 'TeachingClassManage',
        component: () => import('../views/academic/TeachingClassManage.vue'),
        meta: { roles: ['ACADEMIC'] }
      },
      {
        path: 'academic/classes',
        name: 'AcademicClassManage',
        component: () => import('../views/admin/ClassManage.vue'),
        meta: { roles: ['ACADEMIC'] }
      },
      {
        path: 'academic/score-unlock',
        name: 'AcademicScoreUnlock',
        component: () => import('../views/admin/ScoreUnlock.vue'),
        meta: { roles: ['ACADEMIC'] }
      },
      {
        path: 'academic/dashboard',
        name: 'GlobalDashboard',
        component: () => import('../views/academic/GlobalDashboard.vue'),
        meta: { roles: ['ACADEMIC', 'DIRECTOR'] }
      },
      // 专业负责人
      {
        path: 'director/grad-req',
        name: 'GradReqManage',
        component: () => import('../views/director/GradReqManage.vue'),
        meta: { roles: ['DIRECTOR'] }
      },
      {
        path: 'director/macro-matrix',
        name: 'MacroMatrix',
        component: () => import('../views/director/MacroMatrix.vue'),
        meta: { roles: ['DIRECTOR'] }
      },
      {
        path: 'director/global-compute',
        name: 'GlobalCompute',
        component: () => import('../views/director/GlobalCompute.vue'),
        meta: { roles: ['DIRECTOR', 'ACADEMIC'] }
      },
      // 主讲教师
      {
        path: 'teacher/:classId/objectives',
        name: 'ObjectiveSetup',
        component: () => import('../views/teacher/ObjectiveSetup.vue'),
        meta: { roles: ['TEACHER'] }
      },
      {
        path: 'teacher/:classId/weights',
        name: 'WeightAssign',
        component: () => import('../views/teacher/WeightAssign.vue'),
        meta: { roles: ['TEACHER'] }
      },
      {
        path: 'teacher/:classId/assessments',
        name: 'AssessmentMap',
        component: () => import('../views/teacher/AssessmentMap.vue'),
        meta: { roles: ['TEACHER'] }
      },
      {
        path: 'teacher/:classId/questions',
        name: 'QuestionSetup',
        component: () => import('../views/teacher/QuestionSetup.vue'),
        meta: { roles: ['TEACHER'] }
      },
      {
        path: 'teacher/:classId/scores',
        name: 'ScoreImport',
        component: () => import('../views/teacher/ScoreImport.vue'),
        meta: { roles: ['TEACHER'] }
      },
      {
        path: 'teacher/:classId/compute',
        name: 'CourseCompute',
        component: () => import('../views/teacher/CourseCompute.vue'),
        meta: { roles: ['TEACHER'] }
      }
    ]
  }
]

function getHomePath(roleCode) {
  switch (roleCode) {
    case 'ADMIN': return '/admin/users'
    case 'ACADEMIC': return '/academic/courses'
    case 'DIRECTOR': return '/director/grad-req'
    case 'TEACHER': return '/teacher/1/objectives'
    default: return null
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.public) {
    // 已登录用户访问 /login 时，直接跳到首页
    if (token && to.path === '/login') {
      const roleCode = localStorage.getItem('roleCode')
      const home = getHomePath(roleCode)
      if (home) return next(home)
    }
    return next()
  }

  if (!token) {
    return next('/login')
  }

  const roleCode = localStorage.getItem('roleCode')

  // 访问根路径 / 时，按角色跳转
  if (to.path === '/') {
    const home = getHomePath(roleCode)
    return next(home || '/login')
  }

  // 角色不匹配时，跳到该角色首页，并标记无权限提示
  if (to.meta.roles && !to.meta.roles.includes(roleCode)) {
    console.warn('权限检查失败:', { path: to.path, roleCode, requiredRoles: to.meta.roles })
    sessionStorage.setItem('permDenied', to.path)
    const home = getHomePath(roleCode)
    return next(home || '/login')
  }

  next()
})

export default router
