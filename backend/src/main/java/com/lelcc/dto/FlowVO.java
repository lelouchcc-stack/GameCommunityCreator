package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/** 页面逻辑图出参：图返回对象，前端直接喂给图执行器与画布。 */
@Data
public class FlowVO {

    private Long id;

    private Long pageId;

    private String flowKey;

    private String flowName;

    private Map<String, Object> graph;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
