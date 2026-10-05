package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 游戏版本，对应 board_version。
 * 注意：没有 is_current 字段——"当前版本"的唯一事实源是 board.current_version_id。
 */
@Data
@TableName("board_version")
public class BoardVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long boardId;

    /** 版本号，如 1.3.0，同一板块内唯一 */
    private String versionNo;

    private String versionName;

    private LocalDateTime releaseTime;

    private String updateNote;

    /** 1正常 0停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
