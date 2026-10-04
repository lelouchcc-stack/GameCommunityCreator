package com.lelcc.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 页面节点出参。管理端拿到的是节点的原始覆盖值（style/content/behavior），
 * 用户端拿到的是与组件类型默认值合并后的结果；两者共用同一结构，由 children 组成树。
 */
@Data
public class NodeVO {

    private Long id;

    private Long parentId;

    private String typeCode;

    private String typeName;

    private String renderKind;

    private Integer isContainer;

    private Integer isBuiltin;

    private String nodeName;

    private Integer sort;

    private Integer visible;

    private Map<String, Object> style;

    private Map<String, Object> content;

    private Map<String, Object> behavior;

    private String remark;

    private List<NodeVO> children = new ArrayList<>();
}
