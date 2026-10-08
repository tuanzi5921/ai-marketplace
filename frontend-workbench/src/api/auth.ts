import { request } from './request'
import type { AuthUser } from '@/stores/auth'

export interface LoginResult {
  token: string
  user: AuthUser
}

export function localLogin(account: string, password: string) {
  return request.post<LoginResult>('/auth/local/login', { account, password })
}

export function me() {
  return request.get<AuthUser>('/auth/me')
}

export function changePassword(oldPassword: string, newPassword: string) {
  return request.post<void>('/auth/change-password', { oldPassword, newPassword })
}
