package com.gcc.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 更新游戏信息的请求参数。 */
public record UpdateGameInfoRequest(
        @NotBlank(message = "游戏名称不能为空")
        @Size(max = 80, message = "游戏名称长度不能超过 80 个字符")
        String name,

        @Size(max = 160, message = "一句话简介长度不能超过 160 个字符")
        String tagline,

        @Size(max = 3000, message = "游戏简介长度不能超过 3000 个字符")
        String description,

        @Size(max = 300, message = "封面图地址长度不能超过 300 个字符")
        String coverUrl,

        @Size(max = 120, message = "平台信息长度不能超过 120 个字符")
        String platforms,

        @Size(max = 40, message = "发行状态长度不能超过 40 个字符")
        String releaseStatus) {
}
