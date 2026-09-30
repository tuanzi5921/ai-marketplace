package com.company.ai.marketplace.service;

import com.company.ai.marketplace.entity.MpSubmission;
import com.company.ai.marketplace.entity.SysUser;
import com.company.ai.marketplace.mapper.MpSubmissionMapper;
import com.company.ai.marketplace.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知服务（v1 降级为日志记录）。
 * <p>原企微消息推送链路已移除。保留方法签名避免下游调用点报错；
 * 真正的触达通道（邮件 / IM webhook 等）后续在此处接入。
 * <p>所有方法均不阻塞主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MpSubmissionMapper submissionMapper;
    private final SysUserMapper userMapper;

    /** 通知作者：审核结果 */
    public void notifyReviewResult(Long submissionId, String status, String reason) {
        MpSubmission sub = submissionMapper.selectById(submissionId);
        SysUser author = sub == null ? null : userMapper.selectById(sub.getAuthorId());
        log.info("[Notification:log] kind=review-result submission={} author={} status={} reason={}",
                submissionId, author == null ? null : author.getId(), status, reason);
    }

    /** 通知作者：作品收到新评分 */
    public void notifyRated(Long submissionId, Integer ratingCount) {
        MpSubmission sub = submissionMapper.selectById(submissionId);
        SysUser author = sub == null ? null : userMapper.selectById(sub.getAuthorId());
        log.info("[Notification:log] kind=rated submission={} author={} ratingCount={}",
                submissionId, author == null ? null : author.getId(), ratingCount);
    }

    /** 通知作者：作品退回补修 */
    public void notifyReturnForFix(Long submissionId, String reason) {
        MpSubmission sub = submissionMapper.selectById(submissionId);
        SysUser author = sub == null ? null : userMapper.selectById(sub.getAuthorId());
        log.info("[Notification:log] kind=return-for-fix submission={} author={} reason={}",
                submissionId, author == null ? null : author.getId(), reason);
    }

    /**
     * 兼容历史调用：推送纯文本消息，当前仅记录日志。
     */
    public void sendPersonal(String userIdent, String content) {
        log.info("[Notification:log] kind=personal to={} content={}", userIdent, content);
    }

    /** 作品详情页相对路径（前端拼接前缀） */
    private String submissionDetailUrl(Long submissionId) {
        return "/submissions/" + submissionId;
    }
}
