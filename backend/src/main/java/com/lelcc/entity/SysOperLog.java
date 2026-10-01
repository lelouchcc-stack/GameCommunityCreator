package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 操作日志，对应 sys_oper_log。由 OperLogAspect 自动写入。 */
@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 ID（后台账号） */
    private Long userId;

    private String username;

    /** 业务模块，如"板块管理" */
    private String module;

    /** 操作描述，如"新增板块" */
    private String operation;

    /** 请求方法，形如 BoardController.create(..) */
    private String method;

    /** 请求参数 JSON，敏感字段已脱敏 */
    private String params;

    private String ip;

    /** 1成功 0失败 */
    private Integer status;

    private String errorMsg;

    /** 耗时（毫秒） */
    private Long costTime;

    private LocalDateTime createTime;
}
