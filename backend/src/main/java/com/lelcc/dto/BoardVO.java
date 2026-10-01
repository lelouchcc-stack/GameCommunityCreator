package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 板块列表项。比 Board 多一个 currentVersionNo，
 * 管理端表格里直接显示"当前版本 1.3.0"而不是一个 ID。
 */
@Data
public class BoardVO {

    private Long id;

    private String name;

    private String intro;

    private String cover;

    private Integer sort;

    private Integer status;

    private Long currentVersionId;

    /** 关联 board_version.version_no，未设置当前版本时为 null */
    private String currentVersionNo;

    private LocalDateTime createTime;
}
