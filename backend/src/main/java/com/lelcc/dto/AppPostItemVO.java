package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 用户端帖子列表项。expired 由服务层比对帖子版本与板块当前版本后置位。 */
@Data
public class AppPostItemVO {

    private Long id;

    private String title;

    private String cover;

    private Long boardId;

    private String boardName;

    private Long versionId;

    private String versionNo;

    private Long stageId;

    private String stageName;

    private String authorName;

    private String authorAvatar;

    private Integer likeCount;

    private Integer viewCount;

    private Integer commentCount;

    private Integer isTop;

    /** 板块当前版本 ID，仅用于服务层判定 expired，不直接暴露 */
    private Long currentVersionId;

    /** 帖子关联版本与板块当前版本不一致 */
    private Boolean expired;

    /**
     * 帖子状态：0待审 1已发布 2已下架。
     * 公开列表里恒为 1（SQL 已过滤），只有「我的帖子」会用到——作者要看到自己待审/被下架的帖。
     */
    private Integer status;

    /** 审核意见/驳回原因，仅「我的帖子」下发 */
    private String auditRemark;

    private LocalDateTime createTime;
}
