package com.lelcc.service;

import com.lelcc.common.PageResult;
import com.lelcc.dto.ReportVO;

public interface ReportService {

    PageResult<ReportVO> page(Integer status, long pageNum, long pageSize);

    /** 处理或驳回举报；可选同时下架被举报的帖子 */
    void handle(Long id, Integer status, String remark, Boolean offlinePost);
}
