import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 当前登录用户信息
export interface AuthUser {
  id?: string | number
  username?: string
  displayName?: string
  avatarUrl?: string
  department?: string
  role?: string
  points?: number
}

// 鉴权状态：token 与用户信息
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('jwt') || '')
  const user = ref<AuthUser | null>(null)

  // 是否已登录
  const isLoggedIn = computed(() => !!token.value)

  function setToken(value: string) {
    token.value = value
    if (value) {
      localStorage.setItem('jwt', value)
    } else {
      localStorage.removeItem('jwt')
    }
  }

  function setUser(value: AuthUser | null) {
    user.value = value
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('jwt')
  }

  return { token, user, isLoggedIn, setToken, setUser, logout }
})
