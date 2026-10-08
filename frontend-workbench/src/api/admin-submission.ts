import { request, type PageResult } from './request'

export interface AdminSubmission {
  id: string
  title: string
  authorId: number
  shortDesc?: string
  type: string
  businessDomain?: string
  status: string
  rejectReason?: string
  reviewedBy?: number
  reviewedAt?: string
  publishedAt?: string
  downloadCount: number
  ratingAvg: number
  ratingCount: number
  createdAt: string
  coverUrl?: string
}

export function listAllSubmissions(page = 1, size = 10, params: { status?: string; type?: string } = {}) {
  return request.get<PageResult<AdminSubmission>>('/submissions/admin', { params: { page, size, ...params } })
}

export function offlineSubmission(id: string) {
  return request.post<void>(`/submissions/${id}/offline`)
}

export function deleteSubmission(id: string) {
  return request.delete<void>(`/submissions/${id}`)
}
