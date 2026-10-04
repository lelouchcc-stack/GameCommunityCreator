package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 页面逻辑图，对应 ui_flow（uk: page_id + flow_key）。 */
@Data
@TableName("ui_flow")
public class UiFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属页面（逻辑外键 ui_page.id） */
    private Long pageId;

    /** 图标识，如 init / submit */
    private String flowKey;

    private String flowName;

    /** 逻辑图（JSON：nodes + edges + exports） */
    private String graphJson;

    /** 1启用 0停用 */
    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
