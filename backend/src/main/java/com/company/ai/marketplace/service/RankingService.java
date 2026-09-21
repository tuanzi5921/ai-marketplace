package com.company.ai.marketplace.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ai.marketplace.entity.MpCompetition;
import com.company.ai.marketplace.entity.MpSubmission;
import com.company.ai.marketplace.mapper.MpCompetitionMapper;
import com.company.ai.marketplace.mapper.MpSubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final MpSubmissionMapper submissionMapper;
    private final MpCompetitionMapper competitionMapper;

    /** 总榜 Top N（默认 50） */
    public List<MpSubmission> overallTopN(int n) {
        if (n <= 0) n = 50;
        return submissionMapper.selectList(new LambdaQueryWrapper<MpSubmission>()
                .eq(MpSubmission::getStatus, "PUBLISHED")
                .orderByDesc(MpSubmission::getRatingAvg)
                .orderByDesc(MpSubmission::getDownloadCount)
                .last("LIMIT " + n));
    }

    /** 按赛道（business_domain）取 Top N */
    public List<MpSubmission> topNByTrack(String track, int n) {
        if (n <= 0) n = 10;
        return submissionMapper.selectList(new LambdaQueryWrapper<MpSubmission>()
                .eq(MpSubmission::getStatus, "PUBLISHED")
                .eq(MpSubmission::getBusinessDomain, track)
                .orderByDesc(MpSubmission::getRatingAvg)
                .orderByDesc(MpSubmission::getDownloadCount)
                .last("LIMIT " + n));
    }

    /**
     * 按当前 ACTIVE 大赛的 topNPerTrack 聚合各赛道榜单。
     * <p>赛道列表从大赛 tracksConfig JSON 中无法解析时，退化为取所有作品按 business_domain 分组。
     * 简化实现：直接按 DB 中实际出现的 business_domain 分组，每组取 topNPerTrack。
     */
    public Map<String, List<MpSubmission>> byCurrentCompetition() {
        Map<String, List<MpSubmission>> result = new HashMap<>();
        MpCompetition c = competitionMapper.selectOne(new LambdaQueryWrapper<MpCompetition>()
                .eq(MpCompetition::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        int topN = (c != null && c.getTopNPerTrack() != null) ? c.getTopNPerTrack() : 10;

        // 取所有 PUBLISHED 作品的 business_domain 去重
        List<MpSubmission> all = submissionMapper.selectList(new LambdaQueryWrapper<MpSubmission>()
                .eq(MpSubmission::getStatus, "PUBLISHED")
                .select(MpSubmission::getBusinessDomain));
        for (MpSubmission s : all) {
            String domain = StrUtil.isBlank(s.getBusinessDomain()) ? "未分类" : s.getBusinessDomain();
            result.putIfAbsent(domain, topNByTrack(domain, topN));
        }
        // 同时保留总榜
        result.put("__overall__", overallTopN(topN * 5));
        return result;
    }
}
