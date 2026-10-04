package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 组件类型出参：schema 与默认值都返回对象/数组，前端直接用于生成编辑器。 */
@Data
public class ComponentTypeVO {

    private Long id;

    private String typeCode;

    private String typeName;

    private String category;

    private String renderKind;

    private Integer isContainer;

    private Integer isBuiltin;

    private List<String> styleSchema;

    private List<String> contentSchema;

    private List<String> behaviorSchema;

    private Map<String, Object> defaultStyle;

    private Map<String, Object> defaultContent;

    private String previewIcon;

    private Integer sort;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
