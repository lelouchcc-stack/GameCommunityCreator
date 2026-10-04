package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 公告 / 版本更新说明，对应 notice。 */
@Data
@TableName("notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 1公告 2版本更新说明 */
    private Integer type;

    private Integer sort;

    /** 1发布 0草稿 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
