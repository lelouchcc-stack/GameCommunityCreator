package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 标签列表项，带板块名便于后台表格展示。 */
@Data
public class TagVO {

    private Long id;

    private Long boardId;

    /** 关联 board.name，全局标签为 null */
    private String boardName;

    private String tagName;

    private Integer tagType;

    private Integer sort;

    private Integer status;

    private LocalDateTime createTime;
}
