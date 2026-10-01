package com.company.ai.marketplace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ai.marketplace.entity.*;
import com.company.ai.marketplace.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 运营后台数据看板服务。
 * <p>聚合多个表的数据，返回前端看板所需的统计指标。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MpSubmissionMapper submissionMapper;
    private final MpCommentReportMapper commentReportMapper;
    private final MpDownloadLogMapper downloadLogMapper;
    private final SysUserMapper userMapper;
    private final MpCompetitionMapper competitionMapper;

    /**
     * 看板统计：作品总数 / 待审数 / 下载总量 / 用户总数 / 当前大赛名。
     */
    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();

        long totalSubmissions = submissionMapper.selectCount(null);
        long pendingReviews = submissionMapper.selectCount(new LambdaQueryWrapper<MpSubmission>()
                .eq(MpSubmission::getStatus, "PENDING"));
        long totalDownloads = downloadLogMapper.selectCount(null);
        long totalUsers = userMapper.selectCount(null);

        MpCompetition active = competitionMapper.selectOne(new LambdaQueryWrapper<MpCompetition>()
                .eq(MpCompetition::getStatus, "ACTIVE")
                .last("LIMIT 1"));

        result.put("totalSubmissions", totalSubmissions);
        result.put("pendingReviews", pendingReviews);
        result.put("totalDownloads", totalDownloads);
        result.put("totalUsers", totalUsers);
        result.put("activeCompetition", active != null ? active.getName() : null);

        return result;
    }
}
