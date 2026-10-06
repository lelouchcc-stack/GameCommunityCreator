package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 预设出参：schema 与子图都返回对象，前端直接用于画折叠节点与校验。 */
@Data
public class PresetVO {

    private Long id;

    private String presetCode;

    private String presetName;

    private Integer version;

    private Integer isBuiltin;

    private String category;

    private List<Map<String, Object>> inputs;

    private List<Map<String, Object>> outputs;

    private Map<String, Object> graph;

    private Integer status;

    /**
     * 是否已被某个页面逻辑图引用（按 presetCode@version 精确匹配）。
     * 引用中的预设改内容会自动另起新版本、且不允许删除，列表里要能一眼看出来。
     */
    private Boolean referenced;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
