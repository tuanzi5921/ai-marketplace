package com.company.ai.marketplace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ai.marketplace.entity.MpCompetition;
import com.company.ai.marketplace.mapper.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("看板服务测试")
class DashboardServiceTest {

    @Mock
    private MpSubmissionMapper submissionMapper;
    @Mock
    private MpCommentReportMapper commentReportMapper;
    @Mock
    private MpDownloadLogMapper downloadLogMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private MpCompetitionMapper competitionMapper;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("stats 返回所有统计字段")
    void stats_returnsAllFields() {
        when(submissionMapper.selectCount(any())).thenReturn(42L);
        when(downloadLogMapper.selectCount(any())).thenReturn(100L);
        when(userMapper.selectCount(any())).thenReturn(15L);

        MpCompetition active = new MpCompetition();
        active.setName("2026 AI 技能大赛");
        when(competitionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(active);

        Map<String, Object> result = dashboardService.stats();

        assertNotNull(result);
        assertEquals(42L, result.get("totalSubmissions"));
        assertEquals(100L, result.get("totalDownloads"));
        assertEquals(15L, result.get("totalUsers"));
        assertEquals("2026 AI 技能大赛", result.get("activeCompetition"));
    }

    @Test
    @DisplayName("无活跃大赛时 activeCompetition 为 null")
    void stats_noActiveCompetition() {
        when(submissionMapper.selectCount(any())).thenReturn(0L);
        when(downloadLogMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectCount(any())).thenReturn(1L);
        when(competitionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Map<String, Object> result = dashboardService.stats();

        assertNotNull(result);
        assertNull(result.get("activeCompetition"));
        assertEquals(0L, result.get("totalSubmissions"));
    }
}
