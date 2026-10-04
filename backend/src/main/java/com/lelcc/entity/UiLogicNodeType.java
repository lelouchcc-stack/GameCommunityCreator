package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 逻辑层的原子（一棵图能用的节点类型），对应 ui_logic_node_type。管理端只读。 */
@Data
@TableName("ui_logic_node_type")
public class UiLogicNodeType {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原子标识，如 data.postList */
    private String typeCode;

    private String typeName;

    /** trigger/source/operator/condition/action/ui */
    private String kind;

    /** 分组中文名 */
    private String category;

    /** api=调后端接口 / ui=前端本地动作 */
    private String runner;

    /** runner=api 时对应的接口 */
    private String sourceApi;

    /** 可连线输入端口（JSON 数组） */
    private String inputsSchema;

    /** 输出端口（JSON 数组） */
    private String outputsSchema;

    /** 节点自身设置项（JSON 数组，不连线） */
    private String configSchema;

    private String previewIcon;

    private Integer sort;

    /** 1启用 0停用 */
    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
