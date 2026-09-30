import { request } from './request'
import type { AuthUser } from '@/stores/auth'

// 登录响应
export interface LoginResult {
  token: string
  user: AuthUser
}

// 获取当前登录用户
export function me() {
  return request.get<AuthUser>('/auth/me')
}

// 账号密码登录（正式入口）：对应后端 app.auth.local-login-enabled，默认开启
export function localLogin(account: string, password: string) {
  return request.post<LoginResult>('/auth/local/login', { account, password })
}
