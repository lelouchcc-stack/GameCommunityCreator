package com.lelcc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 后台账号新增/编辑请求体。 */
public record SysUserDTO(
        @NotBlank(message = "请输入用户名")
        @Size(max = 50, message = "用户名不能超过 50 个字符")
        String username,

        @Size(max = 50, message = "昵称不能超过 50 个字")
        String nickname,

        @Size(max = 20, message = "手机号不能超过 20 个字符")
        String phone,

        @Size(max = 50, message = "邮箱不能超过 50 个字符")
        String email,

        Integer status,

        @Size(max = 200, message = "备注不能超过 200 个字")
        String remark,

        /** 初始密码，仅新增时使用；编辑请走重置密码接口 */
        String password,

        List<Long> roleIds) {
}
