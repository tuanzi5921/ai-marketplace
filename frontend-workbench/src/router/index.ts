import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('@/views/ChangePassword.vue'),
    meta: { requiresAuth: true, title: '修改密码' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Home', component: () => import('@/views/Home.vue'), meta: { title: '作品列表' } },
      { path: 'submissions/:id', name: 'SubmissionDetail', component: () => import('@/views/SubmissionDetail.vue'), meta: { title: '作品详情' } },
      { path: 'submit', name: 'Submit', component: () => import('@/views/Submit.vue'), meta: { title: '上传作品' } },
      { path: 'rankings', name: 'Rankings', component: () => import('@/views/Ranking.vue'), meta: { title: '排行榜' } },
      { path: 'me', name: 'Me', component: () => import('@/views/Me.vue'), meta: { title: '个人中心' } },
      { path: 'admin/dashboard', name: 'Dashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '数据看板', roles: ['OPERATOR', 'ADMIN'] } },
      { path: 'admin/reviews', name: 'PendingReviews', component: () => import('@/views/admin/PendingReviews.vue'), meta: { title: '待审作品', roles: ['OPERATOR', 'ADMIN', 'JUDGE'] } },
      { path: 'admin/submissions', name: 'SubmissionManagement', component: () => import('@/views/admin/SubmissionManagement.vue'), meta: { title: '作品管理', roles: ['OPERATOR', 'ADMIN'] } },
      { path: 'admin/competitions', name: 'Competitions', component: () => import('@/views/admin/Competitions.vue'), meta: { title: '大赛配置', roles: ['OPERATOR', 'ADMIN'] } },
      { path: 'admin/users', name: 'Users', component: () => import('@/views/admin/Users.vue'), meta: { title: '用户管理', roles: ['ADMIN'] } },
      { path: 'admin/comments', name: 'Comments', component: () => import('@/views/admin/Comments.vue'), meta: { title: '评论管理', roles: ['OPERATOR', 'ADMIN'] } },
      { path: 'admin/judges', name: 'Judges', component: () => import('@/views/admin/Judges.vue'), meta: { title: '评委管理', roles: ['OPERATOR', 'ADMIN'] } },
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0 } }
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (!auth.token) {
    try {
      const raw = localStorage.getItem('ai_market_token')
      if (raw) {
        const token = JSON.parse(raw)
        if (token) auth.setToken(token)
      }
    } catch { /* ignore */ }
  }
  if (!auth.user) {
    try {
      const raw = localStorage.getItem('ai_market_user')
      if (raw) auth.setUser(JSON.parse(raw))
    } catch { /* ignore */ }
  }

  if (to.meta.public) {
    if (to.path === '/login' && auth.isLoggedIn && !auth.needChangePassword) return { path: '/' }
    return true
  }

  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (auth.needChangePassword && to.name !== 'ChangePassword') {
    return { path: '/change-password' }
  }

  if (!auth.needChangePassword && to.name === 'ChangePassword') {
    return { path: '/' }
  }

  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0) {
    if (!auth.hasRole(requiredRoles)) {
      return { path: '/' }
    }
  }

  if (to.meta.title) {
    document.title = `${to.meta.title} | 企业 AI 应用市场`
  }
  return true
})

export default router
