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

// 账号密码登录（兜底）：仅当后端 app.auth.local-login-enabled=true 时可用
export function localLogin(account: string, password: string) {
  return request.post<LoginResult>('/auth/local/login', { account, password })
}

// 跳转企微授权页
// 后端 /auth/wecom/redirect 返回的是 JSON 包络 { data: { redirectUrl } }，
// 不能直接 window.location 导航过去（那样只会看到一坨 JSON），
// 必须先 XHR 取回 redirectUrl 再跳转，与运营后台的实现保持一致。
export async function redirectToWecomAuth() {
  const res = await request.get<{ redirectUrl: string }>('/auth/wecom/redirect')
  if (res?.redirectUrl) {
    window.location.href = res.redirectUrl
  } else {
    throw new Error('未获取到企微授权地址')
  }
}
