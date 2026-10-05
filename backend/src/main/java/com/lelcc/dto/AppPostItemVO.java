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

    private LocalDateTime createTime;
}
