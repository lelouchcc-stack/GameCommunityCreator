package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 首页模块配置，对应 home_module。用户端按 sort 升序渲染，enabled=0 的不渲染。 */
@Data
@TableName("home_module")
public class HomeModule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** banner/notice/hot_post/board_nav，用户端据此选渲染组件 */
    private String moduleType;

    private String moduleName;

    private Integer sort;

    /** 1启用 0停用 */
    private Integer enabled;

    /** 模块私有参数，如热门帖取几条 */
    private String configJson;

    private LocalDateTime updateTime;
}
