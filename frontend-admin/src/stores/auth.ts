import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/** 用户角色枚举 */
export type UserRole = 'OPERATOR' | 'JUDGE' | 'DEPT_HEAD' | 'ADMIN'

/** 登录用户信息 */
export interface AuthUser {
  id: string
  username: string
  displayName: string
  avatar?: string
  department?: string
  points?: number
  roles: UserRole[]
  enabled?: boolean
}

const TOKEN_KEY = 'ai_market_admin_token'
const USER_KEY = 'ai_market_admin_user'

/** 安全读取 localStorage（SSR / 隐私模式兜底） */
function readStorage<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

/**
 * 认证状态 Store
 * 负责 token、当前登录用户的持久化与角色判定
 */
export const useAuthStore = defineStore('auth', () => {
  // —— state ——
  const token = ref<string>(readStorage<string>(TOKEN_KEY, ''))
  const user = ref<AuthUser | null>(readStorage<AuthUser | null>(USER_KEY, null))

  // —— getters ——
  const isLoggedIn = computed(() => !!token.value && !!user.value)

  /** 判断当前用户是否拥有指定角色（传数组为「任一匹配」） */
  function hasRole(role: UserRole | UserRole[]): boolean {
    if (!user.value || !user.value.roles?.length) return false
    const roles = user.value.roles
    if (Array.isArray(role)) return role.some((r) => roles.includes(r))
    return roles.includes(role)
  }

  /** 是否具备运营后台访问角色（OPERATOR / ADMIN） */
  const canAccessAdmin = computed(() => hasRole(['OPERATOR', 'ADMIN']))

  // —— actions ——
  function setToken(value: string) {
    token.value = value
    localStorage.setItem(TOKEN_KEY, JSON.stringify(value))
  }

  function setUser(value: AuthUser | null) {
    user.value = value
    if (value) {
      localStorage.setItem(USER_KEY, JSON.stringify(value))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    user,
    isLoggedIn,
    canAccessAdmin,
    hasRole,
    setToken,
    setUser,
    logout
  }
})
