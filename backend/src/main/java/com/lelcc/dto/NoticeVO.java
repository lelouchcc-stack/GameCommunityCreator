package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 用户端公告列表项。 */
@Data
public class NoticeVO {

    private Long id;

    private String title;

    private String content;

    /** 1公告 2版本更新说明 */
    private Integer type;

    private LocalDateTime createTime;
}
