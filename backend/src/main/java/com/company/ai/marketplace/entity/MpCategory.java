package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 分类标签字典（业务领域/使用场景/类型/技术栈）。 */
@Data
@TableName("mp_category")
public class MpCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** BUSINESS_DOMAIN / USAGE_SCENARIO / TYPE / TECH_STACK */
    private String dim;
    private String code;
    private String name;
    private Integer sortOrder;
    private Integer enabled;
}
