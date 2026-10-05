package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 进度阶段，对应 progress_stage。玩家按阶段筛选适配的攻略。 */
@Data
@TableName("progress_stage")
public class ProgressStage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long boardId;

    /** 阶段名，如新手期/开荒期/中期/满级 */
    private String stageName;

    private String description;

    private Integer sort;

    /** 1正常 0停用 */
    private Integer status;

    private LocalDateTime createTime;
}
