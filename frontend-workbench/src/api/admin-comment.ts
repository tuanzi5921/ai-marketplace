import { request, type PageResult } from './request'

export interface ReportedComment {
  id: string
  content: string
  userId?: number
  submissionId?: number
  reportCount: number
  hidden: boolean
  createdAt?: string
}

export function listReported(page = 1, size = 10) {
  return request.get<PageResult<ReportedComment>>('/comments/reported', { params: { page, size } })
}

export function hideComment(id: string) {
  return request.post<void>(`/comments/${id}/hide`)
}

export function restoreComment(id: string) {
  return request.post<void>(`/comments/${id}/restore`)
}

export function deleteComment(id: string) {
  return request.delete<void>(`/comments/${id}`)
}
