package com.lelcc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 游戏版本新增/编辑请求体。 */
public record BoardVersionDTO(
        @NotBlank(message = "请输入版本号")
        @Size(max = 50, message = "版本号不能超过 50 个字符")
        String versionNo,

        @Size(max = 50, message = "版本名不能超过 50 个字")
        String versionName,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime releaseTime,

        String updateNote) {
}
