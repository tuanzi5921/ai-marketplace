import { http, type PageResult } from './request'

/** 推荐状态 */
export type RecommendationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/** 评委推荐项 */
export interface Recommendation {
  id: string
  recommendeeName: string
  recommendeeId?: string
  recommendDepartment: string
  recommenderName: string
  recommenderId?: string
  reason?: string
  status: RecommendationStatus
  createdAt?: string
}

/**
 * 评委推荐列表
 * GET /api/judges/recommendations?page=&size=
 */
export function listRecommendations(
  page = 1,
  size = 10
): Promise<PageResult<Recommendation>> {
  return http<PageResult<Recommendation>>({
    url: '/judges/recommendations',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 备案入库（通过）
 * POST /api/judges/recommendations/{id}/approve
 */
export function approve(id: string): Promise<void> {
  return http<void>({
    url: `/judges/recommendations/${id}/approve`,
    method: 'post'
  })
}

/**
 * 拒绝
 * POST /api/judges/recommendations/{id}/reject
 */
export function reject(id: string): Promise<void> {
  return http<void>({
    url: `/judges/recommendations/${id}/reject`,
    method: 'post'
  })
}
