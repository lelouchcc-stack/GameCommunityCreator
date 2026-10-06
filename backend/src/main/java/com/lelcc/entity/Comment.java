package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 评论，对应 comment。两级结构：parent_id=被回复的评论，root_id=所属一级评论。 */
@Data
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long memberId;

    /** 0=一级评论，非0=被回复的评论 ID */
    private Long parentId;

    /** 所属一级评论 ID，一级评论自身为 0 */
    private Long rootId;

    private String content;

    private Integer likeCount;

    /** 1正常 0删除 */
    private Integer status;

    private LocalDateTime createTime;
}
