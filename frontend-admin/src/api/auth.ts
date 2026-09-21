import { http } from './request'
import type { AuthUser } from '@/stores/auth'

/** 企微 SSO 登录返回（token 必有；部分后端会同时返回用户） */
export interface LoginResult {
  token: string
  user?: AuthUser
}

/** 企微授权跳转信息 */
export interface WecomRedirect {
  redirectUrl: string
}

/**
 * 企微 SSO 登录：使用授权 code 换取 token
 * POST /api/auth/wecom/login?code=xxx
 */
export function loginByCode(code: string): Promise<LoginResult> {
  return http<LoginResult>({
    url: '/auth/wecom/login',
    method: 'post',
    params: { code }
  })
}

/**
 * 获取当前登录用户信息
 * GET /api/auth/me
 */
export function me(): Promise<AuthUser> {
  return http<AuthUser>({ url: '/auth/me', method: 'get' })
}

/**
 * 跳转企微授权页：拉取授权地址并跳转
 * GET /api/auth/wecom/redirect
 */
export async function redirectToWecomAuth(): Promise<void> {
  const res = await http<WecomRedirect>({ url: '/auth/wecom/redirect', method: 'get' })
  if (res?.redirectUrl) {
    window.location.href = res.redirectUrl
  }
}
