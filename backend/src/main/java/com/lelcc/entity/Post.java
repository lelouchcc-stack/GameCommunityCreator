package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 帖子/攻略，对应 post。 */
@Data
@TableName("post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long boardId;

    /** 作者（member.id） */
    private Long memberId;

    /** 关联游戏版本，可空表示通用帖 */
    private Long versionId;

    /** 关联进度阶段，可空表示通用帖 */
    private Long stageId;

    private String title;

    private String content;

    private String cover;

    /** 图片地址 JSON 数组 */
    private String imageUrls;

    private Integer viewCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer commentCount;

    /** 0待审 1已发布 2已下架 */
    private Integer status;

    /** 审核意见 / 下架原因 */
    private String auditRemark;

    private Integer isTop;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
