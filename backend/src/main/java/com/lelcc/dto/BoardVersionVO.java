package com.lelcc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** 游戏版本列表项。isCurrent 由服务层比对 board.current_version_id 动态得出。 */
@Data
public class BoardVersionVO {

    private Long id;

    private Long boardId;

    private String versionNo;

    private String versionName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime releaseTime;

    private String updateNote;

    private Integer status;

    /** 是否该板块的当前版本（= board.current_version_id） */
    private Boolean isCurrent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
