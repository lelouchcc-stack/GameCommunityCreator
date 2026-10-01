package com.lelcc.service;

import com.lelcc.common.PageResult;
import com.lelcc.entity.SysOperLog;

import java.time.LocalDateTime;
import java.util.List;

public interface SysOperLogService {

    PageResult<SysOperLog> page(String username, String module,
                                LocalDateTime beginTime, LocalDateTime endTime,
                                long pageNum, long pageSize);

    /** 批量删除历史日志 */
    void deleteBatch(List<Long> ids);
}
