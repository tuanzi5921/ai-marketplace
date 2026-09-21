import { request } from './request'
import type { AuthUser } from '@/stores/auth'

// 登录响应
export interface LoginResult {
  token: string
  user: AuthUser
}

// 企微 SSO 登录：用授权码换取 token
export function loginByCode(code: string) {
  return request.post<LoginResult>('/auth/wecom/login', undefined, {
    params: { code }
  })
}

// 获取当前登录用户
export function me() {
  return request.get<AuthUser>('/auth/me')
}

// 跳转企微授权页（前端直接跳转）
export function redirectToWecomAuth() {
  const base = import.meta.env.VITE_API_BASE || ''
  window.location.href = `${base}/api/auth/wecom/redirect`
}
