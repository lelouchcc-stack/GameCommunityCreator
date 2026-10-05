package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 用户端帖子详情。 */
@Data
public class AppPostDetailVO {

    private Long id;

    private String title;

    private String content;

    private String cover;

    /** image_urls 解析后的图片地址列表 */
    private List<String> imageUrls;

    /** image_urls 原始 JSON，仅用于服务层解析，不直接暴露 */
    private String imageUrlsJson;

    private Long boardId;

    private String boardName;

    private Long versionId;

    private String versionNo;

    private Long stageId;

    private String stageName;

    private Long authorId;

    private String authorName;

    private String authorAvatar;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer commentCount;

    private Integer viewCount;

    private Integer isTop;

    /** 帖子状态，仅用于服务层可见性判定 */
    private Integer status;

    private Long currentVersionId;

    private Boolean expired;

    /** 标签名列表 */
    private List<String> tags;

    private LocalDateTime createTime;
}
