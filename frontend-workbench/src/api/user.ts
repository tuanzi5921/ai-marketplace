import { request, type PageResult } from './request'

export interface UserItem {
  id: string
  username: string
  displayName: string
  department?: string
  roles: string[]
  points: number
  enabled: boolean
  avatar?: string
  mustChangePassword?: boolean
}

export function createUser(payload: { email: string; account?: string; username?: string; roles: string[] }) {
  return request.post<UserItem>('/admin/users', payload)
}

export function listUsers(page = 1, size = 10) {
  return request.get<PageResult<UserItem>>('/admin/users', { params: { page, size } })
}

export function grantRole(userId: string, role: string) {
  return request.post<void>(`/admin/users/${userId}/roles`, undefined, { params: { role } })
}

export function revokeRole(userId: string, role: string) {
  return request.delete<void>(`/admin/users/${userId}/roles`, { params: { role } })
}

export function enable(userId: string) {
  return request.post<void>(`/admin/users/${userId}/enable`)
}

export function disable(userId: string) {
  return request.post<void>(`/admin/users/${userId}/disable`)
}
