package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 举报列表项：带出被举报帖子的标题、内容与状态，以及举报人和处理人。 */
@Data
public class ReportVO {

    private Long id;

    private Long postId;

    private String postTitle;

    private String postContent;

    /** 被举报帖子当前状态：0待审 1已发布 2已下架 */
    private Integer postStatus;

    private String reporterName;

    private String reason;

    /** 举报人补充说明 */
    private String description;

    /** 0待处理 1已处理 2已驳回 */
    private Integer status;

    private String handleUserName;

    /** 处理/驳回说明 */
    private String handleRemark;

    private LocalDateTime handleTime;

    private LocalDateTime createTime;
}
