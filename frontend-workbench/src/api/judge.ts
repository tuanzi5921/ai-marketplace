import { request, type PageResult } from './request'

export type RecommendationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface Recommendation {
  id: string
  referrerId?: number
  candidateId?: number
  department?: string
  reason?: string
  status: RecommendationStatus
  createdAt?: string
}

export function listRecommendations(page = 1, size = 10) {
  return request.get<PageResult<Recommendation>>('/judges/recommendations', { params: { page, size } })
}

export function approveRecommendation(id: string) {
  return request.post<void>(`/judges/recommendations/${id}/approve`)
}

export function rejectRecommendation(id: string) {
  return request.post<void>(`/judges/recommendations/${id}/reject`)
}
