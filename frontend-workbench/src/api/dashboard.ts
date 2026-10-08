import { request } from './request'

export interface DashboardStats {
  totalSubmissions: number
  pendingReviews: number
  totalDownloads: number
  totalUsers: number
  activeCompetition?: string | null
}

export function stats() {
  return request.get<DashboardStats>('/admin/dashboard/stats')
}
