package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** SAAS / 在线平台作品的访问信息。 */
@Data
@TableName("mp_saas_link")
public class MpSaasLink {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private String accessUrl;
    private String credentials;
    private String availability;
}
