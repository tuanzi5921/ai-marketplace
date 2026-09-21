import { request } from './request'

// 当前大赛信息（首页倒计时使用）
export interface Competition {
  id?: string | number
  name?: string
  tracks?: string[]
  startTime?: string
  endTime?: string
  status?: string
}

// 获取当前大赛
export function getCurrent() {
  return request.get<Competition | null>('/competitions/current')
}
