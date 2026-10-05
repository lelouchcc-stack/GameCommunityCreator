package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 进度阶段列表项。 */
@Data
public class ProgressStageVO {

    private Long id;

    private Long boardId;

    private String stageName;

    private String description;

    private Integer sort;

    private Integer status;

    private LocalDateTime createTime;
}
