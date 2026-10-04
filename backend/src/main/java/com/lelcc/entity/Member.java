package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家账号，对应 member。
 * 与后台账号 sys_user 是两套独立账号体系：这里只代表社区里的普通玩家，
 * 不参与后台 RBAC 权限。
 */
@Data
@TableName("member")
public class Member {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，全局唯一 */
    private String username;

    /** BCrypt 哈希，不存明文 */
    private String password;

    private String nickname;

    private String avatar;

    private String intro;

    /** 1正常 0封禁 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
