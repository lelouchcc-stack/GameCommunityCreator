package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户端页面（首页 / 自定义页），对应 ui_page。 */
@Data
@TableName("ui_page")
public class UiPage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 页面标识，如 home / about */
    private String pageKey;

    private String pageName;

    /** 用户端路由，如 / 或 /about */
    private String routePath;

    /** 1首页 0普通页 */
    private Integer isHome;

    /** 1启用 0停用 */
    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
