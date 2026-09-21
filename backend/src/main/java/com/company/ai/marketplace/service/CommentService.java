package com.company.ai.marketplace.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.dto.CommentCreateDTO;
import com.company.ai.marketplace.dto.CommentReportDTO;
import com.company.ai.marketplace.entity.MpComment;
import com.company.ai.marketplace.entity.MpCommentReport;
import com.company.ai.marketplace.entity.MpSubmission;
import com.company.ai.marketplace.mapper.MpCommentMapper;
import com.company.ai.marketplace.mapper.MpCommentReportMapper;
import com.company.ai.marketplace.mapper.MpSubmissionMapper;
import com.company.ai.marketplace.security.LoginUser;
import com.company.ai.marketplace.security.ThreadLocalContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论服务（先发后审，举报制）。
 * <p>v1 策略：
 * <ul>
 *   <li>任何登录用户可对 PUBLISHED 作品评论</li>
 *   <li>支持回复（parentId 非空）</li>
 *   <li>举报满 3 次自动隐藏（report_count >= 3 时 hidden=1）</li>
 *   <li>运营可手动隐藏 / 恢复评论</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final MpCommentMapper commentMapper;
    private final MpCommentReportMapper reportMapper;
    private final MpSubmissionMapper submissionMapper;
    private final AuditService auditService;

    /** 举报阈值：满 3 次自动隐藏 */
    private static final int REPORT_THRESHOLD = 3;

    /** 发表评论 */
    @Transactional
    public Long create(CommentCreateDTO dto) {
        LoginUser current = ThreadLocalContext.get();
        if (StrUtil.isBlank(dto.getContent())) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "评论内容不能为空");
        }
        // 作品必须已发布
        MpSubmission sub = submissionMapper.selectById(dto.getSubmissionId());
        if (sub == null || !"PUBLISHED".equals(sub.getStatus())) {
            throw new BizException(ErrorCode.SUBMISSION_NOT_PUBLISHED);
        }
        // 回复对象校验
        if (dto.getParentId() != null) {
            MpComment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null || !parent.getSubmissionId().equals(dto.getSubmissionId())) {
                throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "回复的父评论不存在或不属于该作品");
            }
        }

        MpComment c = new MpComment();
        c.setSubmissionId(dto.getSubmissionId());
        c.setUserId(current.getId());
        c.setParentId(dto.getParentId());
        c.setContent(dto.getContent());
        c.setReportCount(0);
        c.setHidden(0);
        c.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(c);

        auditService.log("COMMENT", "CREATE", "COMMENT", c.getId(),
                "submission=" + dto.getSubmissionId());
        log.info("评论发表: id={} user={} submission={}", c.getId(), current.getId(), dto.getSubmissionId());
        return c.getId();
    }

    /**
     * 查询作品的可见评论列表（hidden=0）。
     * 顶级评论与回复一起返回，前端按 parentId 组装树。
     */
    public List<MpComment> listBySubmission(Long submissionId) {
        return commentMapper.selectList(new LambdaQueryWrapper<MpComment>()
                .eq(MpComment::getSubmissionId, submissionId)
                .eq(MpComment::getHidden, 0)
                .orderByAsc(MpComment::getCreatedAt));
    }

    /** 举报评论：一人一评论只能举报一次，举报满阈值自动隐藏 */
    @Transactional
    public void report(CommentReportDTO dto) {
        LoginUser current = ThreadLocalContext.get();
        MpComment c = commentMapper.selectById(dto.getCommentId());
        if (c == null) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        if (c.getHidden() == 1) return; // 已隐藏无需再举报

        // 一人一评论只能举报一次
        Long existed = reportMapper.selectCount(new LambdaQueryWrapper<MpCommentReport>()
                .eq(MpCommentReport::getCommentId, dto.getCommentId())
                .eq(MpCommentReport::getReporterId, current.getId()));
        if (existed != null && existed > 0) {
            throw new BizException(ErrorCode.DUPLICATE_SUBMIT.getCode(), "已举报过该评论");
        }

        MpCommentReport r = new MpCommentReport();
        r.setCommentId(dto.getCommentId());
        r.setReporterId(current.getId());
        r.setReason(dto.getReason());
        r.setHandled(0);
        r.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(r);

        // 举报计数 +1
        commentMapper.update(null, new LambdaUpdateWrapper<MpComment>()
                .eq(MpComment::getId, dto.getCommentId())
                .setSql("report_count = report_count + 1"));
        // 重新读，判断是否达到阈值
        MpComment reloaded = commentMapper.selectById(dto.getCommentId());
        if (reloaded.getReportCount() >= REPORT_THRESHOLD) {
            commentMapper.update(null, new LambdaUpdateWrapper<MpComment>()
                    .eq(MpComment::getId, dto.getCommentId())
                    .set(MpComment::getHidden, 1));
            log.info("评论因举报达标自动隐藏: id={} count={}", dto.getCommentId(), reloaded.getReportCount());
        }

        auditService.log("COMMENT", "REPORT", "COMMENT", dto.getCommentId(),
                "reporter=" + current.getId() + " reason=" + dto.getReason());
    }

    /** 运营手动隐藏评论 */
    @Transactional
    public void hide(Long commentId) {
        requireOperator();
        commentMapper.update(null, new LambdaUpdateWrapper<MpComment>()
                .eq(MpComment::getId, commentId)
                .set(MpComment::getHidden, 1));
        auditService.log("COMMENT", "HIDE", "COMMENT", commentId, "by operator");
    }

    /** 运营恢复评论（重置举报计数 + hidden=0） */
    @Transactional
    public void restore(Long commentId) {
        requireOperator();
        commentMapper.update(null, new LambdaUpdateWrapper<MpComment>()
                .eq(MpComment::getId, commentId)
                .set(MpComment::getHidden, 0)
                .set(MpComment::getReportCount, 0));
        auditService.log("COMMENT", "RESTORE", "COMMENT", commentId, "by operator");
    }

    /** 删除自己的评论（仅自己可删） */
    @Transactional
    public void deleteOwn(Long commentId) {
        LoginUser current = ThreadLocalContext.get();
        MpComment c = commentMapper.selectById(commentId);
        if (c == null) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!c.getUserId().equals(current.getId())) {
            throw new BizException(ErrorCode.NO_PERMISSION.getCode(), "只能删除自己的评论");
        }
        commentMapper.deleteById(commentId);
        auditService.log("COMMENT", "DELETE", "COMMENT", commentId, "user=" + current.getId());
    }

    // ====== 内部工具 ======

    private void requireOperator() {
        LoginUser u = ThreadLocalContext.get();
        if (u == null || u.getRoles() == null
                || (!u.getRoles().contains("OPERATOR") && !u.getRoles().contains("ADMIN"))) {
            throw new BizException(ErrorCode.NO_PERMISSION.getCode(), "仅运营 / 超管可操作");
        }
    }
}
