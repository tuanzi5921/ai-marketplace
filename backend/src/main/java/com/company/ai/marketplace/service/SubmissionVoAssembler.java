package com.company.ai.marketplace.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ai.marketplace.dto.AuthorVO;
import com.company.ai.marketplace.dto.RankingItemVO;
import com.company.ai.marketplace.dto.SubmissionVO;
import com.company.ai.marketplace.entity.MpSaasLink;
import com.company.ai.marketplace.entity.MpSubmission;
import com.company.ai.marketplace.entity.MpSubmissionArtifact;
import com.company.ai.marketplace.entity.SysUser;
import com.company.ai.marketplace.mapper.MpSaasLinkMapper;
import com.company.ai.marketplace.mapper.MpSubmissionArtifactMapper;
import com.company.ai.marketplace.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 把 {@link MpSubmission} 实体装配成前端要的视图对象。
 * <p>作者、SaaS 链接、最新附件三处关联都按批查询，避免列表接口逐条 N+1。
 */
@Component
@RequiredArgsConstructor
public class SubmissionVoAssembler {

    private final SysUserMapper userMapper;
    private final MpSaasLinkMapper saasLinkMapper;
    private final MpSubmissionArtifactMapper artifactMapper;

    public SubmissionVO toVO(MpSubmission submission) {
        return toVOList(List.of(submission)).get(0);
    }

    public List<SubmissionVO> toVOList(Collection<MpSubmission> submissions) {
        if (submissions == null || submissions.isEmpty()) return List.of();
        Context ctx = load(submissions);
        List<SubmissionVO> result = new ArrayList<>(submissions.size());
        for (MpSubmission s : submissions) {
            SubmissionVO vo = new SubmissionVO();
            fill(vo, s, ctx);
            result.add(vo);
        }
        return result;
    }

    /**
     * 转成榜单项。名次按传入顺序从 1 连续编号——排序已在查询里做完，
     * 这里不再按分数重排，也不做并列名次的合并。
     */
    public List<RankingItemVO> toRankingList(List<MpSubmission> submissions) {
        if (submissions == null || submissions.isEmpty()) return List.of();
        Context ctx = load(submissions);
        List<RankingItemVO> result = new ArrayList<>(submissions.size());
        int rank = 1;
        for (MpSubmission s : submissions) {
            RankingItemVO vo = new RankingItemVO();
            fill(vo, s, ctx);
            vo.setRank(rank++);
            vo.setScore(s.getRatingAvg());
            result.add(vo);
        }
        return result;
    }

    // ====== 内部 ======

    private Context load(Collection<MpSubmission> submissions) {
        Context ctx = new Context();

        Set<Long> authorIds = new HashSet<>();
        Set<Long> submissionIds = new HashSet<>();
        Set<Long> artifactIds = new HashSet<>();
        for (MpSubmission s : submissions) {
            if (s.getAuthorId() != null) authorIds.add(s.getAuthorId());
            if (s.getId() != null) submissionIds.add(s.getId());
            if (s.getLatestArtifactId() != null) artifactIds.add(s.getLatestArtifactId());
        }

        if (!authorIds.isEmpty()) {
            for (SysUser u : userMapper.selectBatchIds(authorIds)) {
                AuthorVO author = new AuthorVO();
                author.setId(u.getId());
                author.setUsername(u.getUsername());
                author.setDisplayName(u.getUsername());
                author.setAvatarUrl(u.getAvatarUrl());
                author.setDepartment(u.getDepartment());
                ctx.authors.put(u.getId(), author);
            }
        }

        if (!submissionIds.isEmpty()) {
            List<MpSaasLink> links = saasLinkMapper.selectList(new LambdaQueryWrapper<MpSaasLink>()
                    .in(MpSaasLink::getSubmissionId, submissionIds));
            for (MpSaasLink link : links) {
                ctx.saasLinks.putIfAbsent(link.getSubmissionId(), link);
            }
        }

        if (!artifactIds.isEmpty()) {
            for (MpSubmissionArtifact a : artifactMapper.selectBatchIds(artifactIds)) {
                ctx.artifacts.put(a.getId(), a);
            }
        }

        return ctx;
    }

    private void fill(SubmissionVO vo, MpSubmission s, Context ctx) {
        vo.setId(s.getId());
        vo.setTitle(s.getTitle());
        vo.setAuthorId(s.getAuthorId());
        vo.setShortDesc(s.getShortDesc());
        vo.setDetailDesc(s.getDetailDesc());
        vo.setType(s.getType());
        vo.setBusinessDomain(s.getBusinessDomain());
        vo.setUsageScenario(s.getUsageScenario());
        vo.setCoverUrl(s.getCoverUrl());
        vo.setVersion(s.getVersion());
        vo.setLatestArtifactId(s.getLatestArtifactId());
        vo.setDownloadCount(s.getDownloadCount());
        vo.setRatingAvg(s.getRatingAvg());
        vo.setRatingCount(s.getRatingCount());
        vo.setStatus(s.getStatus());
        vo.setRejectReason(s.getRejectReason());
        vo.setReviewedBy(s.getReviewedBy());
        vo.setReviewedAt(s.getReviewedAt());
        vo.setPublishedAt(s.getPublishedAt());
        vo.setDeleted(s.getDeleted());
        vo.setCreatedAt(s.getCreatedAt());
        vo.setUpdatedAt(s.getUpdatedAt());

        vo.setTechStack(splitList(s.getTechStack()));
        vo.setTags(splitList(s.getTags()));

        vo.setAvgScore(s.getRatingAvg());
        vo.setRaterCount(s.getRatingCount());
        vo.setTrack(s.getBusinessDomain());
        vo.setAuthor(ctx.authors.get(s.getAuthorId()));

        MpSaasLink link = ctx.saasLinks.get(s.getId());
        if (link != null) {
            vo.setSaasUrl(link.getAccessUrl());
            vo.setSaasCredentials(link.getCredentials());
        }

        MpSubmissionArtifact artifact = ctx.artifacts.get(s.getLatestArtifactId());
        if (artifact != null) {
            vo.setFileName(artifact.getFileName());
            vo.setFileSize(artifact.getFileSizeBytes());
            vo.setContentType(artifact.getContentType());
        }
    }

    /**
     * 提交表单里这两个字段是「多个用逗号分隔」的自由文本，历史上也可能存过 JSON 数组，
     * 两种都吃进来，统一吐 List。
     */
    static List<String> splitList(String raw) {
        if (StrUtil.isBlank(raw)) return List.of();
        String text = raw.trim();
        if (text.startsWith("[")) {
            try {
                return JSONUtil.parseArray(text).stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .map(String::trim)
                        .filter(StrUtil::isNotBlank)
                        .toList();
            } catch (Exception e) {
                // 用户在输入框里打了个 "[" 开头但不是合法 JSON，退回按分隔符切
            }
        }
        return Arrays.stream(text.split("[,，;；、]"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private static class Context {
        final Map<Long, AuthorVO> authors = new HashMap<>();
        final Map<Long, MpSaasLink> saasLinks = new HashMap<>();
        final Map<Long, MpSubmissionArtifact> artifacts = new HashMap<>();
    }
}
