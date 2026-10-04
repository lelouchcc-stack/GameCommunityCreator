package com.lelcc.service;

import com.lelcc.dto.LogicNodeVO;

import java.util.List;

/** 原子库：由代码/种子提供，管理端只读。 */
public interface UiLogicNodeService {

    List<LogicNodeVO> list(Integer status);
}
