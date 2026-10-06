package com.lelcc.dto;

import lombok.Data;

/** 版本分布一行：versionNo 为「（通用帖）」时代表未挂版本的帖子 */
@Data
public class StatVersionVO {

    private String versionNo;

    private Long total;

    /** 1=它是所属板块的当前版本（用来在图表里标"当前"） */
    private Integer isCurrent;
}
