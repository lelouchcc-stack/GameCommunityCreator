package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 原子出参：端口 schema 返回对象列表，前端据此画节点与端口。 */
@Data
public class LogicNodeVO {

    private Long id;

    private String typeCode;

    private String typeName;

    /** trigger/source/operator/condition/action/ui */
    private String kind;

    private String category;

    /** api=调后端接口 / ui=前端本地动作 */
    private String runner;

    private String sourceApi;

    /** 可连线输入端口 */
    private List<Map<String, Object>> inputs;

    /** 输出端口 */
    private List<Map<String, Object>> outputs;

    /** 节点自身设置项 */
    private List<Map<String, Object>> config;

    private String previewIcon;

    private Integer sort;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
