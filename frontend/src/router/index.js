import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/userStore'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../pages/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../pages/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  // Admin routes
  {
    path: '/admin/users',
    name: 'UserManagement',
    component: () => import('../pages/admin/Users.vue'),
    meta: { requiresAuth: true, roles: ['admin'] }
  },
  {
    path: '/admin/dictionary',
    name: 'Dictionary',
    component: () => import('../pages/admin/Dictionary.vue'),
    meta: { requiresAuth: true, roles: ['admin'] }
  },
  // Data management routes
  {
    path: '/data/courses',
    name: 'Courses',
    component: () => import('../pages/data/Courses.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/data/requirements',
    name: 'Requirements',
    component: () => import('../pages/data/Requirements.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/data/students',
    name: 'Students',
    component: () => import('../pages/data/Students.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/data/matrix',
    name: 'Matrix',
    component: () => import('../pages/data/Matrix.vue'),
    meta: { requiresAuth: true }
  },
  // Teaching routes
  {
    path: '/teaching/objectives',
    name: 'Objectives',
    component: () => import('../pages/teaching/Objectives.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/teaching/assessment',
    name: 'Assessment',
    component: () => import('../pages/teaching/Assessment.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/teaching/scores',
    name: 'Scores',
    component: () => import('../pages/teaching/Scores.vue'),
    meta: { requiresAuth: true }
  },
  // Calculation routes
  {
    path: '/calc/progress',
    name: 'Progress',
    component: () => import('../pages/calc/Progress.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/calc/results',
    name: 'Results',
    component: () => import('../pages/calc/Results.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/calc/reports',
    name: 'Reports',
    component: () => import('../pages/calc/Reports.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../pages/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const isAuthenticated = userStore.isAuthenticated

  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else if (to.meta.roles && !to.meta.roles.some(role => userStore.roles.includes(role))) {
    next('/login')
  } else {
    next()
  }
})

export default router


