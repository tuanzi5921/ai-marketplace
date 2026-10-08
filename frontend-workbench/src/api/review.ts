import { request, type PageResult } from './request'

export interface ReviewItem {
  id: string
  title: string
  authorId?: number
  shortDesc?: string
  type: string
  businessDomain?: string
  status: string
  createdAt: string
  coverUrl?: string
}

export function listPending(page = 1, size = 10) {
  return request.get<PageResult<ReviewItem>>('/reviews/pending', { params: { page, size } })
}

export function approve(id: string, reason: string) {
  return request.post<void>(`/reviews/${id}/approve`, undefined, { params: { reason } })
}

export function reject(id: string, reason: string) {
  return request.post<void>(`/reviews/${id}/reject`, undefined, { params: { reason } })
}
