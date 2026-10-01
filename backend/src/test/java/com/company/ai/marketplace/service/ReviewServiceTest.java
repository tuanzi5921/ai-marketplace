package com.company.ai.marketplace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.dto.PageResult;
import com.company.ai.marketplace.entity.MpReviewLog;
import com.company.ai.marketplace.entity.MpSubmission;
import com.company.ai.marketplace.mapper.MpReviewLogMapper;
import com.company.ai.marketplace.mapper.MpSubmissionMapper;
import com.company.ai.marketplace.mapper.SysUserMapper;
import com.company.ai.marketplace.security.LoginUser;
import com.company.ai.marketplace.security.ThreadLocalContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("审核服务测试")
class ReviewServiceTest {

    @Mock
    private MpSubmissionMapper submissionMapper;
    @Mock
    private MpReviewLogMapper reviewLogMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ReviewService reviewService;

    private MpSubmission mockSubmission;

    @BeforeEach
    void setUp() {
        LoginUser reviewer = new LoginUser();
        reviewer.setId(99L);
        reviewer.setUsername("operator");
        ThreadLocalContext.set(reviewer);

        mockSubmission = new MpSubmission();
        mockSubmission.setId(1L);
        mockSubmission.setTitle("测试作品");
        mockSubmission.setAuthorId(10L);
        mockSubmission.setStatus("PENDING");
    }

    @AfterEach
    void tearDown() {
        ThreadLocalContext.clear();
    }

    @Test
    @DisplayName("审核通过：PENDING → PUBLISHED")
    void approve_success() {
        when(submissionMapper.selectById(1L)).thenReturn(mockSubmission);
        when(submissionMapper.update(any(), any())).thenReturn(1);
        when(reviewLogMapper.insert(any(MpReviewLog.class))).thenReturn(1);

        reviewService.approve(1L, "符合要求");

        verify(submissionMapper).selectById(1L);
        verify(submissionMapper).update(any(), any());
        verify(reviewLogMapper).insert(any(MpReviewLog.class));
        verify(auditService).log(eq("REVIEW"), eq("APPROVE"), eq("SUBMISSION"), eq(1L), anyString());
    }

    @Test
    @DisplayName("审核驳回：PENDING → REJECTED")
    void reject_success() {
        when(submissionMapper.selectById(1L)).thenReturn(mockSubmission);
        when(submissionMapper.update(any(), any())).thenReturn(1);
        when(reviewLogMapper.insert(any(MpReviewLog.class))).thenReturn(1);

        reviewService.reject(1L, "内容不合规");

        verify(submissionMapper).selectById(1L);
        verify(submissionMapper).update(any(), any());
        verify(reviewLogMapper).insert(any(MpReviewLog.class));
        verify(auditService).log(eq("REVIEW"), eq("REJECT"), eq("SUBMISSION"), eq(1L), anyString());
    }

    @Test
    @DisplayName("作品不存在时抛 RESOURCE_NOT_FOUND")
    void approve_notFound() {
        when(submissionMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.approve(999L, ""));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
        verify(submissionMapper).selectById(999L);
        verify(submissionMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("非 PENDING 状态时抛 STATUS_MISMATCH")
    void approve_wrongStatus() {
        mockSubmission.setStatus("PUBLISHED");
        when(submissionMapper.selectById(1L)).thenReturn(mockSubmission);

        BizException ex = assertThrows(BizException.class,
                () -> reviewService.approve(1L, ""));

        assertEquals(ErrorCode.STATUS_MISMATCH.getCode(), ex.getCode());
        verify(submissionMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("待审队列分页返回正确结构")
    void pendingQueue_returnsPageResult() {
        Page<MpSubmission> page = new Page<>(1, 10);
        page.setRecords(List.of(mockSubmission));
        page.setTotal(1);
        when(submissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<MpSubmission> result = reviewService.pendingQueue(1, 10);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getPage());
        assertEquals(10L, result.getSize());
    }

    @Test
    @DisplayName("待审队列为空时返回空列表")
    void pendingQueue_empty() {
        Page<MpSubmission> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(submissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<MpSubmission> result = reviewService.pendingQueue(1, 10);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
        assertEquals(0, result.getTotal());
    }
}
