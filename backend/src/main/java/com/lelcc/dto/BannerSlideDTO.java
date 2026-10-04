package com.lelcc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 轮播图新增 / 编辑请求体。 */
public record BannerSlideDTO(
        @Size(max = 50, message = "标题不能超过 50 个字")
        String title,

        @Size(max = 100, message = "副标题不能超过 100 个字")
        String subtitle,

        @NotBlank(message = "请上传轮播图或填写图片地址")
        @Size(max = 255, message = "图片地址不能超过 255 个字符")
        String image,

        @Size(max = 255, message = "跳转地址不能超过 255 个字符")
        String linkUrl,

        @Min(value = 0, message = "排序不能小于 0")
        Integer sort) {
}
