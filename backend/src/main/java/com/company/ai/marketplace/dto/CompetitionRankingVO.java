package com.company.ai.marketplace.dto;

import lombok.Data;

import java.util.List;

/**
 * 大赛聚合榜。
 * <p>与前端 {@code CompetitionRanking} 契约一致：
 * {@code { competitionName, tracks: [{ track, items }], overall }}。
 */
@Data
public class CompetitionRankingVO {

    /** 当前 ACTIVE 大赛名称；无进行中赛事时为 null */
    private String competitionName;

    /** 各赛道榜单，赛道取自作品实际出现的 businessDomain */
    private List<TrackRanking> tracks;

    /** 跨赛道总榜 */
    private List<RankingItemVO> overall;

    @Data
    public static class TrackRanking {
        private String track;
        private List<RankingItemVO> items;
    }
}
