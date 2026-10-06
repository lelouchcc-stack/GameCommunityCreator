package com.lelcc.dto;

import lombok.Data;

/** 趋势图的一天：date 形如 2026-10-05，postTotal / commentTotal 是当天新增数 */
@Data
public class StatTrendVO {

    private String date;

    /** SQL 里聚合列统一叫 total；日期与帖子/评论两条查询共用这个 VO */
    private Long total;
}
