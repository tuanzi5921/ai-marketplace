import { request } from './request'
import type { SubmissionSummary } from './submission'

// 总榜 / 赛道榜返回作品列表（带排名信息）
export type RankingItem = SubmissionSummary & {
  rank?: number
  score?: number
}

// 大赛聚合榜
export interface CompetitionRanking {
  competitionName?: string
  tracks?: { track: string; items: RankingItem[] }[]
  overall?: RankingItem[]
}

// 总榜
export function overall(n = 10) {
  return request.get<RankingItem[]>('/rankings/overall', { params: { n } })
}

// 赛道榜
export function byTrack(track: string, n = 10) {
  return request.get<RankingItem[]>(`/rankings/tracks/${track}`, {
    params: { n }
  })
}

// 大赛聚合榜
export function byCompetition() {
  return request.get<CompetitionRanking>('/rankings/competition')
}
