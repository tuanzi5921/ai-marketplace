package com.company.ai.marketplace.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.dto.CompetitionDTO;
import com.company.ai.marketplace.entity.MpCompetition;
import com.company.ai.marketplace.mapper.MpCompetitionMapper;
import com.company.ai.marketplace.security.LoginUser;
import com.company.ai.marketplace.security.ThreadLocalContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 大赛配置与状态机服务。
 * <p>双模架构：一个平台至多一个 ACTIVE 赛事。
 * <ul>
 *   <li>赛事状态：DRAFT → ACTIVE → JUDGING → CLOSED</li>
 *   <li>阶段 phase：SUBMIT → REVIEW → AWARD（运营手动切换，全期宽松 SOP）</li>
 *   <li>权限：仅超管 ADMIN 可配置 / 激活 / 切换阶段</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionService {

    private final MpCompetitionMapper competitionMapper;
    private final AuditService auditService;

    /** 创建大赛配置（DRAFT 草稿） */
    @Transactional
    public Long create(CompetitionDTO dto) {
        requireAdmin();
        MpCompetition c = new MpCompetition();
        c.setName(dto.getName());
        c.setStatus("DRAFT");
        c.setPhase("SUBMIT");
        c.setSubmitStartAt(dto.getSubmitStartAt());
        c.setSubmitEndAt(dto.getSubmitEndAt());
        c.setReviewAt(dto.getReviewAt());
        c.setShortlistThreshold(dto.getShortlistThreshold() == null ? 50 : dto.getShortlistThreshold());
        c.setTopNPerTrack(dto.getTopNPerTrack() == null ? 10 : dto.getTopNPerTrack());
        c.setAwardsConfig(dto.getAwardsConfig());
        c.setTracksConfig(dto.getTracksConfig());
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        competitionMapper.insert(c);

        auditService.log("COMPETITION", "CREATE", "COMPETITION", c.getId(), "name=" + dto.getName());
        log.info("大赛配置创建: id={} name={}", c.getId(), c.getName());
        return c.getId();
    }

    /** 更新大赛配置（DRAFT 或 ACTIVE 均可编辑） */
    @Transactional
    public void update(CompetitionDTO dto) {
        requireAdmin();
        if (dto.getId() == null) throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "id 不能为空");
        MpCompetition c = competitionMapper.selectById(dto.getId());
        if (c == null) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        if ("CLOSED".equals(c.getStatus())) {
            throw new BizException(ErrorCode.STATUS_MISMATCH.getCode(), "已关闭的大赛不可编辑");
        }
        if (StrUtil.isNotBlank(dto.getName())) c.setName(dto.getName());
        if (dto.getSubmitStartAt() != null) c.setSubmitStartAt(dto.getSubmitStartAt());
        if (dto.getSubmitEndAt() != null) c.setSubmitEndAt(dto.getSubmitEndAt());
        if (dto.getReviewAt() != null) c.setReviewAt(dto.getReviewAt());
        if (dto.getShortlistThreshold() != null) c.setShortlistThreshold(dto.getShortlistThreshold());
        if (dto.getTopNPerTrack() != null) c.setTopNPerTrack(dto.getTopNPerTrack());
        if (dto.getAwardsConfig() != null) c.setAwardsConfig(dto.getAwardsConfig());
        if (dto.getTracksConfig() != null) c.setTracksConfig(dto.getTracksConfig());
        c.setUpdatedAt(LocalDateTime.now());
        competitionMapper.updateById(c);

        auditService.log("COMPETITION", "UPDATE", "COMPETITION", c.getId(), "name=" + c.getName());
    }

    /**
     * 激活大赛：DRAFT → ACTIVE。
     * 同时将平台其他 ACTIVE 赛事置为 CLOSED（保证至多一个 ACTIVE）。
     */
    @Transactional
    public void activate(Long id) {
        requireAdmin();
        MpCompetition c = competitionMapper.selectById(id);
        if (c == null) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!"DRAFT".equals(c.getStatus())) {
            throw new BizException(ErrorCode.STATUS_MISMATCH.getCode(), "仅 DRAFT 状态可激活");
        }
        // 关闭其他 ACTIVE 赛事
        List<MpCompetition> actives = competitionMapper.selectList(new LambdaQueryWrapper<MpCompetition>()
                .eq(MpCompetition::getStatus, "ACTIVE"));
        for (MpCompetition a : actives) {
            a.setStatus("CLOSED");
            a.setUpdatedAt(LocalDateTime.now());
            competitionMapper.updateById(a);
        }
        c.setStatus("ACTIVE");
        c.setPhase("SUBMIT");
        c.setUpdatedAt(LocalDateTime.now());
        competitionMapper.updateById(c);

        auditService.log("COMPETITION", "ACTIVATE", "COMPETITION", id, "name=" + c.getName());
        log.info("大赛激活: id={}", id);
    }

    /**
     * 切换大赛阶段（全期宽松 SOP）。
     * <p>SUBMIT → REVIEW → AWARD，仅允许顺序前进，不允许回退。
     */
    @Transactional
    public void switchPhase(Long id, String targetPhase) {
        requireAdmin();
        MpCompetition c = competitionMapper.selectById(id);
        if (c == null) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!"ACTIVE".equals(c.getStatus()) && !"JUDGING".equals(c.getStatus())) {
            throw new BizException(ErrorCode.STATUS_MISMATCH.getCode(), "仅 ACTIVE/JUDGING 可切换阶段");
        }
        String current = c.getPhase();
        if (!isValidTransition(current, targetPhase)) {
            throw new BizException(ErrorCode.STATUS_MISMATCH.getCode(),
                    "非法阶段切换: " + current + " → " + targetPhase);
        }
        c.setPhase(targetPhase);
        // 切到 AWARD 时，赛事状态同步置为 CLOSED（颁奖后转入常驻）
        if ("AWARD".equals(targetPhase)) {
            c.setStatus("CLOSED");
        } else if ("REVIEW".equals(targetPhase)) {
            c.setStatus("JUDGING");
        }
        c.setUpdatedAt(LocalDateTime.now());
        competitionMapper.updateById(c);

        auditService.log("COMPETITION", "SWITCH_PHASE", "COMPETITION", id,
                current + " → " + targetPhase);
        log.info("大赛阶段切换: id={} {} → {}", id, current, targetPhase);
    }

    /** 获取当前 ACTIVE 大赛（前端倒计时 / 大赛页头使用） */
    public MpCompetition current() {
        return competitionMapper.selectOne(new LambdaQueryWrapper<MpCompetition>()
                .eq(MpCompetition::getStatus, "ACTIVE")
                .last("LIMIT 1"));
    }

    /** 获取所有大赛配置（运营后台列表） */
    public List<MpCompetition> listAll() {
        return competitionMapper.selectList(new LambdaQueryWrapper<MpCompetition>()
                .orderByDesc(MpCompetition::getCreatedAt));
    }

    /** 详情 */
    public MpCompetition detail(Long id) {
        MpCompetition c = competitionMapper.selectById(id);
        if (c == null) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        return c;
    }

    // ====== 内部工具 ======

    private boolean isValidTransition(String from, String to) {
        if (from == null || to == null) return false;
        if ("SUBMIT".equals(from) && "REVIEW".equals(to)) return true;
        if ("REVIEW".equals(from) && "AWARD".equals(to)) return true;
        return false;
    }

    private void requireAdmin() {
        LoginUser u = ThreadLocalContext.get();
        if (u == null || u.getRoles() == null || !u.getRoles().contains("ADMIN")) {
            throw new BizException(ErrorCode.NO_PERMISSION.getCode(), "仅超管可操作大赛配置");
        }
    }
}
