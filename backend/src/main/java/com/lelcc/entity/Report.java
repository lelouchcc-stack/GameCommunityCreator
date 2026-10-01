package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 举报记录，对应 report。 */
@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    /** 举报人（member.id） */
    private Long memberId;

    private String reason;

    /** 举报人补充说明 */
    private String description;

    /** 0待处理 1已处理 2已驳回 */
    private Integer status;

    /** 处理人（sys_user.id） */
    private Long handleUserId;

    private LocalDateTime handleTime;

    /** 处理/驳回说明 */
    private String handleRemark;

    private LocalDateTime createTime;
}
