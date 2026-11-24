package com.gcc.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 更新站点设置的请求参数。 */
public record UpdateSettingsRequest(
        @NotBlank(message = "themeMode 不能为空")
        @Pattern(regexp = "DARK|LIGHT|SYSTEM", message = "themeMode 只能是 DARK、LIGHT 或 SYSTEM")
        String themeMode) {
}
