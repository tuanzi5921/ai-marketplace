package com.company.ai.marketplace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.dto.RatingDTO;
import com.company.ai.marketplace.entity.*;
import com.company.ai.marketplace.mapper.*;
import com.company.ai.marketplace.security.LoginUser;
import com.company.ai.marketplace.security.ThreadLocalContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 多维评分服务。
 * <p>规则：
 * <ul>
 *   <li>防刷：必须先下载才能评分（硬约束）</li>
 *   <li>一人一评：已评则覆盖更新</li>
 *   <li>权重：评委 30% + 员工 70%</li>
 *   <li>维度：易用性 / 业务价值 / 技术质量 / 创新性（各 1-5）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {

    private final MpRatingMapper ratingMapper;
    private final MpSubmissionMapper submissionMapper;
    private final SubmissionService submissionService;

    /** 评委权重 0.3，员工权重 0.7 */
    private static final BigDecimal JUDGE_WEIGHT = new BigDecimal("0.3");
    private static final BigDecimal USER_WEIGHT  = new BigDecimal("0.7");

    @Transactional
    public void rate(RatingDTO dto) {
        LoginUser current = ThreadLocalContext.get();

        // 1. 防刷校验：必须先下载
        if (!submissionService.hasDownloaded(current.getId(), dto.getSubmissionId())) {
            throw new BizException(ErrorCode.MUST_DOWNLOAD_FIRST);
        }

        // 2. 作品必须已发布
        MpSubmission sub = submissionMapper.selectById(dto.getSubmissionId());
        if (sub == null || !"PUBLISHED".equals(sub.getStatus())) {
            throw new BizException(ErrorCode.SUBMISSION_NOT_PUBLISHED);
        }

        // 3. 一人一评：已评则覆盖
        MpRating existing = ratingMapper.selectOne(new LambdaQueryWrapper<MpRating>()
                .eq(MpRating::getUserId, current.getId())
                .eq(MpRating::getSubmissionId, dto.getSubmissionId()));

        boolean isJudge = current.getRoles() != null && current.getRoles().contains("JUDGE");
        String role = isJudge ? "JUDGE" : "USER";

        if (existing != null) {
            // 覆盖更新
            existing.setEaseOfUse(dto.getEaseOfUse());
            existing.setBusinessValue(dto.getBusinessValue());
            existing.setTechQuality(dto.getTechQuality());
            existing.setInnovation(dto.getInnovation());
            existing.setCommentText(dto.getCommentText());
            existing.setRatedByRole(role);
            existing.setRatedAt(LocalDateTime.now());
            ratingMapper.updateById(existing);
        } else {
            MpRating rating = new MpRating();
            rating.setSubmissionId(dto.getSubmissionId());
            rating.setUserId(current.getId());
            rating.setRatedByRole(role);
            rating.setEaseOfUse(dto.getEaseOfUse());
            rating.setBusinessValue(dto.getBusinessValue());
            rating.setTechQuality(dto.getTechQuality());
            rating.setInnovation(dto.getInnovation());
            rating.setCommentText(dto.getCommentText());
            rating.setRatedAt(LocalDateTime.now());
            ratingMapper.insert(rating);
        }

        // 4. 重算综合评分（评委 3 + 员工 7 权重）
        recalculateRatingAvg(sub.getId());

        log.info("评分提交: user={} submission={} role={}", current.getId(), sub.getId(), role);
    }

    /**
     * 重新计算作品的综合评分（评委 3 + 员工 7 权重）。
     * <p>4 维度各自取评委均值 × 0.3 + 员工均值 × 0.7，再 4 维度取均值。
     */
    void recalculateRatingAvg(Long submissionId) {
        List<MpRating> all = ratingMapper.selectList(new LambdaQueryWrapper<MpRating>()
                .eq(MpRating::getSubmissionId, submissionId));

        if (all.isEmpty()) return;

        // 评委评分均值
        List<MpRating> judgeRatings = all.stream().filter(r -> "JUDGE".equals(r.getRatedByRole())).toList();
        List<MpRating> userRatings  = all.stream().filter(r -> "USER".equals(r.getRatedByRole())).toList();

        BigDecimal judgeAvg = avgOf4Dimensions(judgeRatings);
        BigDecimal userAvg  = avgOf4Dimensions(userRatings);

        BigDecimal combined;
        if (!judgeRatings.isEmpty() && !userRatings.isEmpty()) {
            combined = judgeAvg.multiply(JUDGE_WEIGHT).add(userAvg.multiply(USER_WEIGHT));
        } else if (!judgeRatings.isEmpty()) {
            combined = judgeAvg;
        } else {
            combined = userAvg;
        }

        // 更新作品
        submissionMapper.update(null, new LambdaUpdateWrapper<MpSubmission>()
                .eq(MpSubmission::getId, submissionId)
                .set(MpSubmission::getRatingAvg, combined.setScale(2, RoundingMode.HALF_UP))
                .set(MpSubmission::getRatingCount, all.size()));
    }

    /** 4 维度均值 */
    private BigDecimal avgOf4Dimensions(List<MpRating> ratings) {
        if (ratings.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (MpRating r : ratings) {
            sum = sum.add(BigDecimal.valueOf(r.getEaseOfUse()))
                    .add(BigDecimal.valueOf(r.getBusinessValue()))
                    .add(BigDecimal.valueOf(r.getTechQuality()))
                    .add(BigDecimal.valueOf(r.getInnovation()));
        }
        // 4 维度 × N 人 → 除以 (4 × N)
        return sum.divide(BigDecimal.valueOf(4L * ratings.size()), 4, RoundingMode.HALF_UP);
    }
}
