package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 原子团/预设：一段可复用的子图，对应 ui_preset（uk: preset_code + version）。 */
@Data
@TableName("ui_preset")
public class UiPreset {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预设标识，如 preset.login */
    private String presetCode;

    private String presetName;

    /** 版本，引用方锁定版本 */
    private Integer version;

    /** 1内置（只读，可复制为自定义） */
    private Integer isBuiltin;

    private String category;

    /** 预设入参（JSON 数组） */
    private String inputsSchema;

    /** 预设出参（JSON 数组） */
    private String outputsSchema;

    /** 子图（JSON：nodes + edges + exports） */
    private String graphJson;

    /** 1启用 0停用 */
    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
