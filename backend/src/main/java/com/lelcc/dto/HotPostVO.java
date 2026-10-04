package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端首页热门帖子项。
 * 板块名与作者昵称靠左联接带出来，前端一次请求就能把卡片画全（不用再逐条查作者）。
 */
@Data
public class HotPostVO {

    private Long id;

    private String title;

    private String cover;

    private Long boardId;

    private String boardName;

    private String authorName;

    private String authorAvatar;

    private Integer likeCount;

    private Integer viewCount;

    private Integer commentCount;

    /** 是否置顶：置顶帖排在热门前面 */
    private Integer isTop;

    private LocalDateTime createTime;
}
