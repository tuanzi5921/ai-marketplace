package com.company.ai.marketplace.dto;

import lombok.Data;

/**
 * 作品作者摘要，由 sys_user join 得到。
 * <p>不含 account / roles / points 等与「展示作者」无关的字段。
 */
@Data
public class AuthorVO {

    private Long id;

    /** 员工姓名 */
    private String username;

    /** 与 username 同值，前端两套页面各读一个名字 */
    private String displayName;

    private String avatarUrl;

    private String department;
}
