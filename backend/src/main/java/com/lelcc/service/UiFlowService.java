package com.lelcc.service;

import com.lelcc.dto.FlowDTO;
import com.lelcc.dto.FlowVO;

import java.util.List;

/** 页面逻辑图：一个页面可有多张图（init/submit...），按 page+flowKey 整图保存。 */
public interface UiFlowService {

    List<FlowVO> listByPage(Long pageId);

    void save(Long pageId, String flowKey, FlowDTO dto);
}
