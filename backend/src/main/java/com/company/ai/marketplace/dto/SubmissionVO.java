package com.company.ai.marketplace.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 作品视图对象：实体字段 + 前端真正消费的派生字段。
 *
 * <p><b>契约：实体已有的字段名一律保留、不改名</b>，管理端页面（作品管理 / 待审作品）
 * 是按实体口径写的，改名会把它们弄坏。派生字段只做「追加」，因此
 * {@code ratingAvg} 与 {@code avgScore}、{@code ratingCount} 与 {@code raterCount}、
 * {@code businessDomain} 与 {@code track} 是同时存在的两套名字，值相同。
 *
 * <p>两处例外，是有意为之的类型收敛：{@code tags} / {@code techStack} 在实体里是
 * 逗号分隔（或 JSON 文本）的字符串，前端一直按数组消费（{@code v-for} / {@code slice}），
 * 字符串也有 {@code length} 所以不报错、只是渲染成乱码片段。这里在后端拆成
 * {@code List<String>} 再返回，键名不变。
 *
 * <p>新增实体列时记得同步到这里，否则该列不会出现在接口响应中。
 */
@Data
public class SubmissionVO {

    // ===== 与 mp_submission 实体同名的字段 =====
    private Long id;
    private String title;
    private Long authorId;
    private String shortDesc;
    private String detailDesc;
    /** SOURCE / EXECUTABLE / SAAS / DOCUMENT */
    private String type;
    private String businessDomain;
    private String usageScenario;
    private String coverUrl;
    private String version;
    private Long latestArtifactId;
    private Integer downloadCount;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    /** DRAFT/PENDING/APPROVED/REJECTED/PUBLISHED/UNLISTED/FROZEN */
    private String status;
    private String rejectReason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 原实体里是字符串，这里已拆成数组 */
    private List<String> techStack;
    /** 原实体里是字符串，这里已拆成数组 */
    private List<String> tags;

    // ===== 追加的派生字段 =====

    /** 综合评分，等于 ratingAvg；未评分时为 null */
    private BigDecimal avgScore;

    /** 评分人数，等于 ratingCount */
    private Integer raterCount;

    /** 赛道，等于 businessDomain */
    private String track;

    /** 作者信息，join sys_user 得到；作者已注销时为 null */
    private AuthorVO author;

    /** SAAS 作品的访问地址，取自 mp_saas_link */
    private String saasUrl;

    /** SAAS 作品的访问凭据，取自 mp_saas_link */
    private String saasCredentials;

    /** 最新附件的原始文件名 */
    private String fileName;

    /** 最新附件字节数 */
    private Long fileSize;

    /** 最新附件 MIME 类型 */
    private String contentType;
}
