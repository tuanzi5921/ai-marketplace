import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 路由表：登录 / 首页 / 作品详情 / 上传 / 排行榜 / 个人中心
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '作品列表' }
      },
      {
        path: 'submissions/:id',
        name: 'SubmissionDetail',
        component: () => import('@/views/SubmissionDetail.vue'),
        meta: { title: '作品详情' }
      },
      {
        path: 'submit',
        name: 'Submit',
        component: () => import('@/views/Submit.vue'),
        meta: { title: '上传作品' }
      },
      {
        path: 'rankings',
        name: 'Rankings',
        component: () => import('@/views/Ranking.vue'),
        meta: { title: '排行榜' }
      },
      {
        path: 'me',
        name: 'Me',
        component: () => import('@/views/Me.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 全局前置守卫：无 token 一律跳转登录页
const WHITE_LIST = ['/login']
router.beforeEach((to) => {
  const auth = useAuthStore()
  // 同步一次 localStorage，避免刷新丢失 token
  if (!auth.token) {
    const persisted = localStorage.getItem('jwt')
    if (persisted) auth.setToken(persisted)
  }
  if (to.meta.requiresAuth !== false && !auth.isLoggedIn && !WHITE_LIST.includes(to.path)) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && auth.isLoggedIn) {
    return { path: '/' }
  }
  if (to.meta.title) {
    document.title = `${to.meta.title} | 企业 AI 应用市场`
  }
  return true
})

export default router
