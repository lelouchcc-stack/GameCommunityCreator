package com.lelcc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 玩家发帖请求体（对应「发布攻略帖」）。
 *
 * <p>boardId 与 versionId 都必填：社区是按"游戏版本 × 进度阶段"组织内容的，
 * 帖子不挂版本就没法在正确的版本下被看到——这是本项目的核心设计，所以版本不做可选。
 * stageId（进度阶段）与 tagIds（标签）可选。</p>
 */
public record PostCreateDTO(
        @NotNull(message = "请选择所属板块")
        Long boardId,

        @NotNull(message = "请选择关联的游戏版本")
        Long versionId,

        /** 进度阶段，可空表示通用 */
        Long stageId,

        @NotBlank(message = "请输入标题")
        @Size(max = 100, message = "标题不能超过 100 个字")
        String title,

        @NotBlank(message = "请输入正文")
        @Size(max = 20000, message = "正文不能超过 20000 个字")
        String content,

        /** 封面：对象存储 objectKey 或完整地址，可空 */
        String cover,

        /** 标签 id，可空 */
        List<Long> tagIds) {
}
