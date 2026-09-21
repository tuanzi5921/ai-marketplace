import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 运营后台路由表
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      {
        path: 'reviews/pending',
        name: 'PendingReviews',
        component: () => import('@/views/PendingReviews.vue'),
        meta: { title: '待审作品', icon: 'DocumentChecked' }
      },
      {
        path: 'submissions',
        name: 'SubmissionManagement',
        component: () => import('@/views/SubmissionManagement.vue'),
        meta: { title: '作品管理', icon: 'Files' }
      },
      {
        path: 'competitions',
        name: 'Competitions',
        component: () => import('@/views/Competitions.vue'),
        meta: { title: '大赛配置', icon: 'Trophy' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/Users.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'comments',
        name: 'Comments',
        component: () => import('@/views/Comments.vue'),
        meta: { title: '评论管理', icon: 'ChatDotRound' }
      },
      {
        path: 'judges',
        name: 'Judges',
        component: () => import('@/views/Judges.vue'),
        meta: { title: '评委推荐', icon: 'Medal' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

const APP_TITLE = '企业 AI 应用市场 — 运营后台'

router.beforeEach((to) => {
  const auth = useAuthStore()

  // 设置页签标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} · ${APP_TITLE}`
  } else {
    document.title = APP_TITLE
  }

  // 公开路由直接放行
  if (to.meta?.public) {
    // 已登录用户访问登录页 → 回到首页
    if (to.name === 'Login' && auth.isLoggedIn && auth.canAccessAdmin) {
      return { name: 'Dashboard' }
    }
    return true
  }

  // 未登录 → 跳登录
  if (!auth.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  // 已登录但无运营后台角色 → 跳登录
  if (!auth.canAccessAdmin) {
    return { name: 'Login' }
  }

  return true
})

export default router
