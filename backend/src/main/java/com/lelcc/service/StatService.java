package com.lelcc.service;

import com.lelcc.dto.StatOverviewVO;

/** 后台数据统计（只读聚合）。 */
public interface StatService {

    /**
     * 总览：累计快照 + 近 days 天的新增与趋势 + 热门板块 + 版本分布。
     * days 会被收敛到 [7, 90]，默认 30。
     */
    StatOverviewVO overview(Integer days);
}
