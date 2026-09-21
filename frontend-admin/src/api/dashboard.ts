import { http } from './request'

/** 看板统计数据 */
export interface DashboardStats {
  totalSubmissions: number
  pendingReviews: number
  totalDownloads: number
  totalUsers: number
  activeCompetition?: string | null
}

/**
 * 看板统计
 * GET /api/admin/dashboard/stats
 * 注：后端若暂未聚合，可由前端合并多个接口实现，此处统一返回该结构。
 */
export function stats(): Promise<DashboardStats> {
  return http<DashboardStats>({
    url: '/admin/dashboard/stats',
    method: 'get'
  })
}
