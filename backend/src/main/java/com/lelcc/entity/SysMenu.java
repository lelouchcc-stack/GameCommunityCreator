package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 后台菜单，对应 sys_menu。三级结构：M目录 / C菜单 / F按钮。 */
@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 0 = 根 */
    private Long parentId;

    private String menuName;

    /** M目录 C菜单 F按钮 */
    private String menuType;

    private String path;

    /** 前端组件路径，目录(M)为空 */
    private String component;

    /** 权限标识，如 post:audit */
    private String perms;

    private String icon;

    private Integer sort;

    private Integer visible;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
