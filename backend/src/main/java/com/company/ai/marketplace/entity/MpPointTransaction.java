package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 贡献积分流水。 */
@Data
@TableName("mp_point_transaction")
public class MpPointTransaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer delta;
    private Integer balance;
    /** SUBMIT/DOWNLOAD_THRESHOLD/RATING_THRESHOLD/ADMIN_ADJUST */
    private String reason;
    private Long refId;
    private LocalDateTime createdAt;
}
