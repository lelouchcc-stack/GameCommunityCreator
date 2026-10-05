package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 标签，对应 tag。board_id 为空表示全局标签。 */
@Data
@TableName("tag")
public class Tag {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属板块，null = 全局标签 */
    private Long boardId;

    private String tagName;

    /** 1内容类型(攻略/讨论/同人) 2主题 */
    private Integer tagType;

    private Integer sort;

    /** 1正常 0停用 */
    private Integer status;

    private LocalDateTime createTime;
}
