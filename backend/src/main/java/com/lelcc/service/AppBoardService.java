package com.lelcc.service;

import com.lelcc.dto.AppBoardDetailVO;

/** 用户端板块只读：板块详情 + 版本 + 进度阶段。 */
public interface AppBoardService {

    /** 板块详情；板块不存在或已关闭时抛"该板块暂未开放" */
    AppBoardDetailVO detail(Long boardId);
}
