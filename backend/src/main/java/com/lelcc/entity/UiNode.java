package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户端页面节点（树），对应 ui_node；取代原 home_module。 */
@Data
@TableName("ui_node")
public class UiNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pageId;

    /** 父节点，null=顶层 */
    private Long parentId;

    private String typeCode;

    private String nodeName;

    private Integer sort;

    /** 1显示 0隐藏 */
    private Integer visible;

    private String styleJson;

    private String contentJson;

    private String behaviorJson;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
