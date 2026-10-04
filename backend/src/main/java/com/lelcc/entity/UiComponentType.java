package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 组件库中的一种组件类型（模板），对应 ui_component_type。 */
@Data
@TableName("ui_component_type")
public class UiComponentType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String typeCode;

    private String typeName;

    /** base/layout/display/interactive/business */
    private String category;

    /** 渲染内核，业务组件为 builtin:xxx */
    private String renderKind;

    /** 1可容纳子节点 */
    private Integer isContainer;

    /** 1业务组件（代码实现） */
    private Integer isBuiltin;

    /** 开放的样式属性 key 数组（JSON） */
    private String styleSchema;

    private String contentSchema;

    private String behaviorSchema;

    private String defaultStyle;

    private String defaultContent;

    private String previewIcon;

    private Integer sort;

    /** 1启用 0停用 */
    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
