<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { me as fetchMe } from '@/api/auth'
import {
  HomeFilled,
  Upload,
  Trophy,
  User,
  ArrowDown
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 拉取用户信息（首次进入）
onMounted(async () => {
  if (auth.isLoggedIn && !auth.user) {
    try {
      const user = await fetchMe()
      auth.setUser(user)
    } catch {
      // 静默：request 拦截器已处理 401
    }
  }
})

// 激活的菜单项
const activeMenu = computed(() => '/' + (route.path.split('/')[1] || ''))

// 顶部菜单
const menus = [
  { index: '/', label: '首页', icon: HomeFilled },
  { index: '/submit', label: '上传作品', icon: Upload },
  { index: '/rankings', label: '排行榜', icon: Trophy },
  { index: '/me', label: '个人中心', icon: User }
]

function goMenu(index: string) {
  router.push(index)
}

// 用户名展示
const displayName = computed(() => {
  const u = auth.user
  return u?.displayName || u?.username || '我的'
})

const avatarText = computed(() => {
  const name = displayName.value
  return name ? name.charAt(0).toUpperCase() : 'U'
})

function handleCommand(command: string) {
  if (command === 'me') router.push('/me')
  else if (command === 'logout') {
    auth.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="layout-root">
    <header class="app-header">
      <div class="app-header__inner">
        <div class="brand" @click="goMenu('/')">
          <span class="brand__logo">AI</span>
          <span class="brand__name">企业 AI 应用市场</span>
          <span class="brand__sub">· 工作台</span>
        </div>

        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          class="top-menu"
          :ellipsis="false"
          background-color="transparent"
          text-color="#e8ecf7"
          active-text-color="#ffffff"
          @select="goMenu"
        >
          <el-menu-item v-for="m in menus" :key="m.index" :index="m.index">
            <el-icon><component :is="m.icon" /></el-icon>
            <span>{{ m.label }}</span>
          </el-menu-item>
        </el-menu>

        <div class="user-zone">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-trigger">
              <el-avatar :size="32" class="user-avatar">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ displayName }}</span>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="me">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="app-main">
      <router-view />
    </main>

    <footer class="app-footer">
      <span>© {{ new Date().getFullYear() }} 企业 AI 应用市场 · 员工端工作台</span>
    </footer>
  </div>
</template>

<style scoped>
.layout-root {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--brand-bg);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: linear-gradient(120deg, #1e3a8a 0%, #3b5bdb 60%, #4c6ef5 100%);
  box-shadow: 0 2px 16px rgba(30, 58, 138, 0.18);
}

.app-header__inner {
  max-width: 1240px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 24px;
  gap: 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}
.brand__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-weight: 700;
  letter-spacing: 0.5px;
  backdrop-filter: blur(4px);
}
.brand__name {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.brand__sub {
  color: rgba(255, 255, 255, 0.65);
  font-size: 13px;
}

.top-menu {
  flex: 1;
  border-bottom: none !important;
}
.top-menu :deep(.el-menu-item) {
  height: 64px;
  line-height: 64px;
  border-bottom: 3px solid transparent !important;
  font-size: 15px;
}
.top-menu :deep(.el-menu-item.is-active) {
  border-bottom-color: #14b8a6 !important;
  background: rgba(255, 255, 255, 0.08) !important;
}
.top-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
}

.user-zone {
  margin-left: auto;
}
.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #fff;
  outline: none;
}
.user-avatar {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  font-weight: 600;
}
.user-name {
  font-size: 14px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-arrow {
  font-size: 12px;
  opacity: 0.8;
}

.app-main {
  flex: 1;
  width: 100%;
}

.app-footer {
  text-align: center;
  padding: 18px 0;
  color: var(--brand-muted);
  font-size: 13px;
  background: transparent;
}
</style>
