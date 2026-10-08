import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/** 用户角色枚举 */
export type UserRole = 'USER' | 'OPERATOR' | 'JUDGE' | 'DEPT_HEAD' | 'ADMIN'

/** 登录用户信息 */
export interface AuthUser {
  id: string | number
  username: string
  displayName?: string
  avatar?: string
  department?: string
  points?: number
  roles: string[]
  enabled?: boolean
  /** 首次登录是否需改密 */
  mustChangePassword?: boolean
}

const TOKEN_KEY = 'ai_market_token'
const USER_KEY = 'ai_market_user'

/** 安全读取 localStorage（SSR / 隐私模式兜底） */
function readStorage<T>(key: string, fallback: T): T {
  let raw: string | null = null
  try {
    raw = localStorage.getItem(key)
    if (!raw) return fallback
    // token 存为 JSON 字符串，兼容旧格式
    if (typeof fallback === 'string') return (JSON.parse(raw) as string) as unknown as T
    return JSON.parse(raw) as T
  } catch {
    // 兼容直接存的字符串 token
    return (raw as unknown as T) || fallback
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(readStorage<string>(TOKEN_KEY, ''))
  const user = ref<AuthUser | null>(readStorage<AuthUser | null>(USER_KEY, null))

  const isLoggedIn = computed(() => !!token.value)

  const needChangePassword = computed(() => !!user.value?.mustChangePassword)

  function hasRole(role: string | string[]): boolean {
    if (!user.value || !user.value.roles?.length) return false
    const roles = user.value.roles
    if (Array.isArray(role)) return role.some((r) => roles.includes(r))
    return roles.includes(role)
  }

  const canAccessAdmin = computed(() => hasRole(['OPERATOR', 'ADMIN']))
  const canReview = computed(() => hasRole(['OPERATOR', 'ADMIN', 'JUDGE']))
  const canScore = computed(() => hasRole(['JUDGE', 'DEPT_HEAD']))
  const isAdmin = computed(() => hasRole('ADMIN'))

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
    needChangePassword,
    canAccessAdmin,
    canReview,
    canScore,
    isAdmin,
    hasRole,
    setToken,
    setUser,
    logout
  }
})
