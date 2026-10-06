package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 评论列表项。一级评论带 replies（该一级评论下的全部回复）。 */
@Data
public class CommentVO {

    private Long id;

    private Long postId;

    private Long memberId;

    private String authorName;

    private String authorAvatar;

    private Long parentId;

    private Long rootId;

    private String content;

    private Integer likeCount;

    private LocalDateTime createTime;

    /** 二级回复（仅一级评论有） */
    private List<CommentVO> replies;
}
