package com.company.ai.marketplace.service;

import com.company.ai.marketplace.config.AppProperties;
import com.company.ai.marketplace.entity.MpSubmission;
import com.company.ai.marketplace.entity.SysUser;
import com.company.ai.marketplace.integration.wecom.WecomClient;
import com.company.ai.marketplace.mapper.MpSubmissionMapper;
import com.company.ai.marketplace.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 企微消息推送服务（v1 个人应用消息）。
 * <p>v1 触发场景：
 * <ul>
 *   <li>审核结果通知（通过/拒绝）— 文本卡片</li>
 *   <li>退回提醒（作者补修后重提）— 文本卡片</li>
 *   <li>评分提醒（提醒作者有人评分）— 文本卡片</li>
 * </ul>
 * <p>实现策略：
 * <ul>
 *   <li>配置开关 wecom.message.enabled：默认 false，关闭时仅记日志不真实推送</li>
 *   <li>关闭状态下不抛异常，不阻塞主流程</li>
 *   <li>开启后调用集成层 WecomClient 真实推送个人应用消息</li>
 *   <li>推送失败仍不阻塞主流程（评分、审核已正常完成）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MpSubmissionMapper submissionMapper;
    private final SysUserMapper userMapper;
    private final WecomClient wecomClient;
    private final AppProperties appProperties;

    /** 企微消息推送开关，默认关闭（v1 隔离） */
    @Value("${wecom.message.enabled:false}")
    private boolean messageEnabled;

    /** 推送目标为提交作者：审核结果（文本卡片） */
    public void notifyReviewResult(Long submissionId, String status, String reason) {
        MpSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) return;
        SysUser author = userMapper.selectById(sub.getAuthorId());
        if (author == null) return;

        boolean published = "PUBLISHED".equals(status);
        String title = published ? "作品审核通过" : "作品已退回";
        String description = String.format(
                "%s\n状态：%s\n%s",
                sub.getTitle(),
                published ? "已通过并发布到应用市场" : "未通过审核，请修改后重新提交",
                reason == null ? "" : "审核意见：" + reason
        );
        sendCard(author.getWecomUserid(), title, description, submissionDetailUrl(submissionId));
    }

    /** 推送目标为提交作者：评分提醒（文本卡片） */
    public void notifyRated(Long submissionId, Integer ratingCount) {
        MpSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) return;
        SysUser author = userMapper.selectById(sub.getAuthorId());
        if (author == null) return;

        sendCard(author.getWecomUserid(),
                "作品收到新评分",
                String.format("%s\n当前累计评分次数：%d\n快去看看反馈吧～", sub.getTitle(), ratingCount),
                submissionDetailUrl(submissionId));
    }

    /** 推送目标为提交作者：退回补修提醒（文本卡片） */
    public void notifyReturnForFix(Long submissionId, String reason) {
        MpSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) return;
        SysUser author = userMapper.selectById(sub.getAuthorId());
        if (author == null) return;

        sendCard(author.getWecomUserid(),
                "作品退回补修",
                String.format("%s\n请根据审核意见修改后重新提交。\n退回原因：%s", sub.getTitle(), reason),
                submissionDetailUrl(submissionId));
    }

    /**
     * 通用：推送纯文本消息（兼容历史调用）。
     */
    public void sendPersonal(String wecomUserid, String content) {
        if (!messageEnabled) {
            log.info("[Notification:disabled] to={} content={}", wecomUserid, content);
            return;
        }
        if (!wecomClient.isConfigured()) {
            log.warn("[Notification:skip] wecom not configured, to={} content={}", wecomUserid, content);
            return;
        }
        try {
            wecomClient.sendTextMessage(wecomUserid, content);
        } catch (Exception e) {
            log.error("企微消息推送失败 to={} err={}", wecomUserid, e.getMessage(), e);
        }
    }

    /** 推送文本卡片消息 */
    private void sendCard(String toUser, String title, String description, String url) {
        if (!messageEnabled) {
            log.info("[Notification:disabled] to={} title={} desc={}", toUser, title, description);
            return;
        }
        if (!wecomClient.isConfigured()) {
            log.warn("[Notification:skip] wecom not configured, to={} title={}", toUser, title);
            return;
        }
        try {
            wecomClient.sendTextCard(toUser, title, description, url);
        } catch (Exception e) {
            log.error("企微文本卡片推送失败 to={} title={} err={}", toUser, title, e.getMessage(), e);
        }
    }

    /** 作品详情页 URL（前端 H5 路由） */
    private String submissionDetailUrl(Long submissionId) {
        String callback = appProperties.getWecom().getAuthCallback();
        String base = callback == null ? "" : callback.replaceAll("/oauth/callback$", "");
        return base + "/submissions/" + submissionId;
    }
}
