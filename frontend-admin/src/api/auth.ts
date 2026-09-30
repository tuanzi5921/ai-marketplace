import { http } from './request'
import type { AuthUser } from '@/stores/auth'

/** 登录返回（token 与 user 均必返） */
export interface LoginResult {
  token: string
  user: AuthUser
}

/**
 * 获取当前登录用户信息
 * GET /api/auth/me
 */
export function me(): Promise<AuthUser> {
  return http<AuthUser>({ url: '/auth/me', method: 'get' })
}

/**
 * 账号密码登录（正式入口）
 * POST /api/auth/local/login
 */
export function localLogin(account: string, password: string): Promise<LoginResult> {
  return http<LoginResult>({
    url: '/auth/local/login',
    method: 'post',
    data: { account, password }
  })
}

/**
 * 修改密码（首次登录强制改密 / 自助改密）
 * POST /api/auth/change-password
 */
export function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  return http<void>({
    url: '/auth/change-password',
    method: 'post',
    data: { oldPassword, newPassword }
  })
}
