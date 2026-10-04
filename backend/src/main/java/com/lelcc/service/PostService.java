package com.lelcc.service;

import com.lelcc.dto.HotPostVO;

import java.util.List;

/** 帖子读取：目前只有首页热门榜，帖子详情/列表等随玩家侧模块补上。 */
public interface PostService {

    /** 热门帖子（置顶优先，再按点赞、浏览降序），最多 limit 条 */
    List<HotPostVO> listHot(int limit);
}
