import { http, type PageResult } from './request'
import type { UserRole } from '@/stores/auth'

/** 用户项 */
export interface UserItem {
  id: string
  username: string
  displayName: string
  department?: string
  roles: UserRole[]
  points: number
  enabled: boolean
  avatar?: string
}

/**
 * 用户列表
 * GET /api/admin/users?page=&size=
 */
export function listUsers(page = 1, size = 10): Promise<PageResult<UserItem>> {
  return http<PageResult<UserItem>>({
    url: '/admin/users',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 授予角色
 * POST /api/admin/users/{id}/roles?role=xxx
 */
export function grantRole(userId: string, role: UserRole): Promise<void> {
  return http<void>({
    url: `/admin/users/${userId}/roles`,
    method: 'post',
    params: { role }
  })
}

/**
 * 撤销角色
 * DELETE /api/admin/users/{id}/roles?role=xxx
 */
export function revokeRole(userId: string, role: UserRole): Promise<void> {
  return http<void>({
    url: `/admin/users/${userId}/roles`,
    method: 'delete',
    params: { role }
  })
}

/**
 * 启用用户
 * POST /api/admin/users/{id}/enable
 */
export function enable(userId: string): Promise<void> {
  return http<void>({ url: `/admin/users/${userId}/enable`, method: 'post' })
}

/**
 * 停用用户
 * POST /api/admin/users/{id}/disable
 */
export function disable(userId: string): Promise<void> {
  return http<void>({ url: `/admin/users/${userId}/disable`, method: 'post' })
}
