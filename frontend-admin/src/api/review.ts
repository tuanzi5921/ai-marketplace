import { http, type PageResult } from './request'

/** 待审作品项 */
export interface ReviewItem {
  id: string
  title: string
  authorName: string
  authorDepartment?: string
  type: string
  track?: string
  submittedAt: string
  description?: string
  coverUrl?: string
  demoUrl?: string
  status?: string
}

/**
 * 待审作品列表
 * GET /api/reviews/pending?page=&size=
 */
export function listPending(page = 1, size = 10): Promise<PageResult<ReviewItem>> {
  return http<PageResult<ReviewItem>>({
    url: '/reviews/pending',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 审核通过
 * POST /api/reviews/{id}/approve?reason=xxx
 */
export function approve(id: string, reason: string): Promise<void> {
  return http<void>({
    url: `/reviews/${id}/approve`,
    method: 'post',
    params: { reason }
  })
}

/**
 * 审核退回
 * POST /api/reviews/{id}/reject?reason=xxx
 */
export function reject(id: string, reason: string): Promise<void> {
  return http<void>({
    url: `/reviews/${id}/reject`,
    method: 'post',
    params: { reason }
  })
}
