package com.company.ai.marketplace.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.dto.SubmissionCreateDTO;
import com.company.ai.marketplace.entity.*;
import com.company.ai.marketplace.integration.storage.StorageService;
import com.company.ai.marketplace.mapper.*;
import com.company.ai.marketplace.security.LoginUser;
import com.company.ai.marketplace.security.ThreadLocalContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 作品管线：提交 / 查询 / 下载 / 版本迭代。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final MpSubmissionMapper submissionMapper;
    private final MpSubmissionArtifactMapper artifactMapper;
    private final MpSaasLinkMapper saasLinkMapper;
    private final MpDownloadLogMapper downloadLogMapper;
    private final StorageService storageService;
    private final PointService pointService;
    private final AuditService auditService;

    /**
     * 提交作品（上传文件 + 建主记录）。
     * SAAS 类型不要求文件，但要求 saasUrl。
     */
    @Transactional
    public Long submit(SubmissionCreateDTO dto, MultipartFile file) {
        LoginUser current = ThreadLocalContext.get();

        // SAAS 类型校验
        if ("SAAS".equals(dto.getType()) && StrUtil.isBlank(dto.getSaasUrl())) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "SAAS 类型作品必须填写访问 URL");
        }
        // 非 SAAS 类型必须上传文件
        if (!"SAAS".equals(dto.getType()) && (file == null || file.isEmpty())) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "非 SAAS 类型作品必须上传文件");
        }

        // 1. 建作品主记录
        MpSubmission sub = new MpSubmission();
        sub.setTitle(dto.getTitle());
        sub.setAuthorId(current.getId());
        sub.setShortDesc(dto.getShortDesc());
        sub.setDetailDesc(dto.getDetailDesc());
        sub.setType(dto.getType());
        sub.setBusinessDomain(dto.getBusinessDomain());
        sub.setUsageScenario(dto.getUsageScenario());
        sub.setTechStack(dto.getTechStack());
        sub.setCoverUrl(dto.getCoverUrl());
        sub.setTags(dto.getTags());
        sub.setVersion(StrUtil.isBlank(dto.getVersion()) ? "1.0.0" : dto.getVersion());
        sub.setDownloadCount(0);
        sub.setRatingCount(0);
        sub.setStatus("PENDING"); // 提交即入待审队列
        submissionMapper.insert(sub);

        // 2. 存储附件（非 SAAS 类型）
        if (file != null && !file.isEmpty()) {
            StorageService.StoredFile sf = storageService.store(file);
            MpSubmissionArtifact artifact = new MpSubmissionArtifact();
            artifact.setSubmissionId(sub.getId());
            artifact.setVersion(sub.getVersion());
            artifact.setFileName(sf.getFileName());
            artifact.setStoredPath(sf.getStoredPath());
            artifact.setFileSizeBytes(sf.getSizeBytes());
            artifact.setContentType(sf.getContentType());
            artifact.setFileHash(sf.getHash());
            artifact.setIsLatest(1);
            artifactMapper.insert(artifact);

            sub.setLatestArtifactId(artifact.getId());
            submissionMapper.updateById(sub);
        }

        // 3. SAAS 类型存访问链接
        if ("SAAS".equals(dto.getType())) {
            MpSaasLink link = new MpSaasLink();
            link.setSubmissionId(sub.getId());
            link.setAccessUrl(dto.getSaasUrl());
            link.setCredentials(dto.getSaasCredentials());
            link.setAvailability("OK");
            saasLinkMapper.insert(link);
        }

        // 4. 积分：提交作品 +5
        pointService.award(current.getId(), 5, "SUBMIT", sub.getId());

        // 5. 审计
        auditService.log("SUBMISSION", "SUBMIT", "SUBMISSION", sub.getId(),
                "title=" + dto.getTitle() + " type=" + dto.getType());

        log.info("作品提交: id={} author={} title={}", sub.getId(), current.getId(), dto.getTitle());
        return sub.getId();
    }

    /**
     * 分页查询已发布作品列表（公开浏览）。
     */
    public Page<MpSubmission> listPublished(int page, int size, String type, String domain) {
        LambdaQueryWrapper<MpSubmission> w = new LambdaQueryWrapper<>();
        w.eq(MpSubmission::getStatus, "PUBLISHED");
        if (StrUtil.isNotBlank(type)) w.eq(MpSubmission::getType, type);
        if (StrUtil.isNotBlank(domain)) w.eq(MpSubmission::getBusinessDomain, domain);
        w.orderByDesc(MpSubmission::getDownloadCount);
        return submissionMapper.selectPage(new Page<>(page, size), w);
    }

    /**
     * 查询作品详情。
     */
    public MpSubmission detail(Long id) {
        MpSubmission sub = submissionMapper.selectById(id);
        if (sub == null) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        return sub;
    }

    /**
     * 下载作品：记录下载日志 + download_count+1。
     * 返回最新版本附件（用于流式输出）。
     */
    @Transactional
    public MpSubmissionArtifact download(Long submissionId) {
        LoginUser current = ThreadLocalContext.get();
        MpSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null || !"PUBLISHED".equals(sub.getStatus())) {
            throw new BizException(ErrorCode.SUBMISSION_NOT_PUBLISHED);
        }

        // 取最新版本附件
        MpSubmissionArtifact artifact = artifactMapper.selectById(sub.getLatestArtifactId());
        if (artifact == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND.getCode(), "作品附件不存在");
        }

        // 记录下载日志
        MpDownloadLog log = new MpDownloadLog();
        log.setSubmissionId(submissionId);
        log.setArtifactId(artifact.getId());
        log.setUserId(current.getId());
        downloadLogMapper.insert(log);

        // download_count + 1
        submissionMapper.update(null, new LambdaUpdateWrapper<MpSubmission>()
                .eq(MpSubmission::getId, submissionId)
                .setSql("download_count = download_count + 1"));

        auditService.log("SUBMISSION", "DOWNLOAD", "SUBMISSION", submissionId,
                "user=" + current.getId());
        return artifact;
    }

    /**
     * 检查某用户是否已下载过某作品（评分前置条件）。
     */
    public boolean hasDownloaded(Long userId, Long submissionId) {
        Long count = downloadLogMapper.selectCount(new LambdaQueryWrapper<MpDownloadLog>()
                .eq(MpDownloadLog::getUserId, userId)
                .eq(MpDownloadLog::getSubmissionId, submissionId));
        return count != null && count > 0;
    }
}
