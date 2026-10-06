package com.lelcc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 发表评论/回复请求体。parentId 为空表示一级评论。 */
public record CommentDTO(
        @NotNull(message = "帖子 ID 不能为空")
        Long postId,

        /** 被回复的评论 ID，空=一级评论 */
        Long parentId,

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 1000, message = "评论内容不能超过 1000 个字")
        String content) {
}
