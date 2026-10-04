package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 页面出参。 */
@Data
public class PageVO {

    private Long id;

    private String pageKey;

    private String pageName;

    private String routePath;

    private Integer isHome;

    private Integer status;

    private String remark;

    /** 该页面下的节点数（含子节点） */
    private Integer nodeCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
