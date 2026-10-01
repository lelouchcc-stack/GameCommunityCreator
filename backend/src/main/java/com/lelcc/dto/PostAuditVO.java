package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 帖子审核列表项：带出板块、作者、关联版本与进度阶段，管理员不用再点进详情才能判断。 */
@Data
public class PostAuditVO {

    private Long id;

    private String title;

    private String content;

    private String boardName;

    private String authorName;

    /** 关联版本号，通用帖为 null */
    private String versionNo;

    /** 关联进度阶段名，通用帖为 null */
    private String stageName;

    /** 0待审 1已发布 2已下架 */
    private Integer status;

    /** 审核意见 / 下架原因 */
    private String auditRemark;

    private LocalDateTime createTime;
}
