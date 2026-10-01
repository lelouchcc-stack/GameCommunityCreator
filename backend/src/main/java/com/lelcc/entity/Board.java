package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 板块（= 一款游戏的分区），对应 board。 */
@Data
@TableName("board")
public class Board {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String intro;

    private String cover;

    private Integer sort;

    /** 1开放 0关闭 */
    private Integer status;

    /** 当前版本 ID，是"当前版本"的唯一事实源 */
    private Long currentVersionId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
