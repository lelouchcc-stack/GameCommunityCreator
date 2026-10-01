package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 后台账号列表项。比 SysUser 多一个角色名，列表里直接显示"属于哪些角色"。 */
@Data
public class SysUserVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String email;

    private Integer status;

    private String remark;

    /** 逗号拼接的角色名称，仅用于列表展示 */
    private String roleNames;

    private LocalDateTime createTime;
}
