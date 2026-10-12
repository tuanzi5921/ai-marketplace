package com.company.ai.marketplace.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ai.marketplace.dto.CompetitionRankingVO;
import com.company.ai.marketplace.dto.RankingItemVO;
import com.company.ai.marketplace.entity.MpCompetition;
import com.company.ai.marketplace.entity.MpSubmission;
import com.company.ai.marketplace.mapper.MpCompetitionMapper;
import com.company.ai.marketplace.mapper.MpSubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 排行榜服务。
 * <p>排序规则：
 * <ol>
 *   <li>综合评分（rating_avg）降序</li>
 *   <li>下载量（download_count）降序</li>
 * </ol>
 * <p>能力：
 * <ul>
 *   <li>overallTopN：全平台总榜</li>
 *   <li>topNByTrack：按赛道（business_domain）取 Top N</li>
 *   <li>byCurrentCompetition：按当前大赛的 topNPerTrack 聚合各赛道榜单</li>
 * </ul>
 * <p>名次由 {@link SubmissionVoAssembler} 按已排好序的结果从 1 连续编号，并列不合并。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    /** business_domain 为空的作品归入这个赛道名 */
    private static final String UNCLASSIFIED = "未分类";

    private final MpSubmissionMapper submissionMapper;
    private final MpCompetitionMapper competitionMapper;
    private final SubmissionVoAssembler voAssembler;

    /** 总榜 Top N（默认 50） */
    public List<RankingItemVO> overallTopN(int n) {
        if (n <= 0) n = 50;
        return voAssembler.toRankingList(submissionMapper.selectList(
                rankedQuery().last("LIMIT " + n)));
    }

    /** 按赛道（business_domain）取 Top N */
    public List<RankingItemVO> topNByTrack(String track, int n) {
        if (n <= 0) n = 10;
        LambdaQueryWrapper<MpSubmission> w = rankedQuery();
        if (UNCLASSIFIED.equals(track)) {
            w.and(q -> q.isNull(MpSubmission::getBusinessDomain)
                    .or().eq(MpSubmission::getBusinessDomain, ""));
        } else {
            w.eq(MpSubmission::getBusinessDomain, track);
        }
        return voAssembler.toRankingList(submissionMapper.selectList(w.last("LIMIT " + n)));
    }

    /**
     * 按当前 ACTIVE 大赛的 topNPerTrack 聚合各赛道榜单。
     * <p>赛道列表取自作品里实际出现的 business_domain（大赛的 tracksConfig JSON 未参与，
     * 配置了赛道但还没人投稿的赛道不会出现在榜单里）。无 ACTIVE 大赛时按 10 条兜底。
     */
    public CompetitionRankingVO byCurrentCompetition() {
        MpCompetition c = competitionMapper.selectOne(new LambdaQueryWrapper<MpCompetition>()
                .eq(MpCompetition::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        int topN = (c != null && c.getTopNPerTrack() != null) ? c.getTopNPerTrack() : 10;

        Set<String> domains = new LinkedHashSet<>();
        for (MpSubmission s : submissionMapper.selectList(new LambdaQueryWrapper<MpSubmission>()
                .eq(MpSubmission::getStatus, "PUBLISHED")
                .select(MpSubmission::getBusinessDomain)
                .orderByAsc(MpSubmission::getBusinessDomain))) {
            domains.add(StrUtil.isBlank(s.getBusinessDomain()) ? UNCLASSIFIED : s.getBusinessDomain());
        }

        List<CompetitionRankingVO.TrackRanking> tracks = new ArrayList<>(domains.size());
        for (String domain : domains) {
            CompetitionRankingVO.TrackRanking tr = new CompetitionRankingVO.TrackRanking();
            tr.setTrack(domain);
            tr.setItems(topNByTrack(domain, topN));
            tracks.add(tr);
        }

        CompetitionRankingVO vo = new CompetitionRankingVO();
        vo.setCompetitionName(c != null ? c.getName() : null);
        vo.setTracks(tracks);
        vo.setOverall(overallTopN(topN * 5));
        return vo;
    }

    private LambdaQueryWrapper<MpSubmission> rankedQuery() {
        return new LambdaQueryWrapper<MpSubmission>()
                .eq(MpSubmission::getStatus, "PUBLISHED")
                .orderByDesc(MpSubmission::getRatingAvg)
                .orderByDesc(MpSubmission::getDownloadCount);
    }
}
