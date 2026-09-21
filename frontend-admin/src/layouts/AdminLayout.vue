<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Odometer,
  DocumentChecked,
  Files,
  Trophy,
  User,
  ChatDotRound,
  Medal,
  Fold,
  Expand,
  ArrowDown,
  SwitchButton,
  UserFilled
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

// —— 路由 / 状态 ——
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 侧边栏折叠
const collapse = ref(false)

// 菜单项定义（与 router 子路由一一对应）
interface MenuItem {
  index: string
  title: string
  icon: any
}
const menus: MenuItem[] = [
  { index: '/dashboard', title: '仪表盘', icon: Odometer },
  { index: '/reviews/pending', title: '待审作品', icon: DocumentChecked },
  { index: '/submissions', title: '作品管理', icon: Files },
  { index: '/competitions', title: '大赛配置', icon: Trophy },
  { index: '/users', title: '用户管理', icon: User },
  { index: '/comments', title: '评论管理', icon: ChatDotRound },
  { index: '/judges', title: '评委推荐', icon: Medal }
]

// 当前激活菜单
const activeMenu = computed(() => route.path)

// 当前页面标题
const currentTitle = computed(() => (route.meta?.title as string) || '')

// 用户展示信息
const displayName = computed(
  () => auth.user?.displayName || auth.user?.username || '运营管理员'
)
const roleText = computed(() => auth.user?.roles?.join(' / ') || '-')

// 用户下拉操作
async function onCommand(command: string) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录吗？', '提示', {
        type: 'warning',
        confirmButtonText: '退出',
        cancelButtonText: '取消'
      })
      auth.logout()
      ElMessage.success('已退出登录')
      router.push({ name: 'Login' })
    } catch {
      /* 用户取消 */
    }
  }
}
</script>

<template>
  <el-container class="admin-layout">
    <!-- 左侧深色侧边栏 -->
    <el-aside :width="collapse ? '64px' : '220px'" class="sidebar">
      <div class="brand">
        <div class="brand-mark">AI</div>
        <span v-show="!collapse" class="brand-text">应用市场后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapse"
        :collapse-transition="false"
        router
        background-color="transparent"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
        class="side-menu"
      >
        <el-menu-item
          v-for="m in menus"
          :key="m.index"
          :index="m.index"
        >
          <el-icon><component :is="m.icon" /></el-icon>
          <template #title>{{ m.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="topbar">
        <div class="topbar-left">
          <el-icon class="collapse-btn" @click="collapse = !collapse">
            <component :is="collapse ? Expand : Fold" />
          </el-icon>
          <span class="page-crumb">{{ currentTitle }}</span>
        </div>
        <div class="topbar-right">
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-trigger">
              <el-avatar :size="30" class="user-avatar">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <span class="user-name">{{ displayName }}</span>
              <el-icon class="user-caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <span class="muted">角色：{{ roleText }}</span>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  height: 100vh;
}

.sidebar {
  background: var(--sidebar-bg);
  background-image: linear-gradient(180deg, var(--sidebar-bg) 0%, var(--sidebar-bg-deep) 100%);
  transition: width 0.22s ease;
  overflow-x: hidden;
}

.brand {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.brand-mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--brand-primary) 0%, #818cf8 100%);
  color: #fff;
  font-weight: 700;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

.brand-text {
  color: #f8fafc;
  font-weight: 600;
  font-size: 15px;
  white-space: nowrap;
}

.side-menu {
  border-right: none;
  padding: 10px 8px;
}

.side-menu :deep(.el-menu-item) {
  border-radius: 8px;
  margin: 4px 0;
  height: 44px;
  line-height: 44px;
}

.side-menu :deep(.el-menu-item:hover) {
  background-color: var(--sidebar-menu-hover) !important;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: var(--sidebar-menu-active) !important;
  color: #fff !important;
  font-weight: 500;
}

.topbar {
  height: 56px;
  background: var(--card-bg);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  font-size: 20px;
  color: var(--text-regular);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: background 0.15s ease;
}

.collapse-btn:hover {
  background: var(--brand-primary-soft);
  color: var(--brand-primary);
}

.page-crumb {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.topbar-right {
  display: flex;
  align-items: center;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.15s ease;
}

.user-trigger:hover {
  background: var(--brand-primary-soft);
}

.user-avatar {
  background: var(--brand-primary-soft);
  color: var(--brand-primary);
}

.user-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.user-caret {
  font-size: 12px;
  color: var(--text-secondary);
}

.content {
  background: var(--content-bg);
  padding: 0;
  overflow-y: auto;
}
</style>
