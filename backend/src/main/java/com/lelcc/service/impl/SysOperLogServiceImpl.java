package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lelcc.common.BusinessException;
import com.lelcc.common.PageResult;
import com.lelcc.entity.SysOperLog;
import com.lelcc.mapper.SysOperLogMapper;
import com.lelcc.service.SysOperLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysOperLogServiceImpl implements SysOperLogService {

    @Resource
    private SysOperLogMapper sysOperLogMapper;

    @Override
    public PageResult<SysOperLog> page(String username, String module,
                                       LocalDateTime beginTime, LocalDateTime endTime,
                                       long pageNum, long pageSize) {
        LambdaQueryWrapper<SysOperLog> wrapper = Wrappers.<SysOperLog>lambdaQuery()
                .like(StringUtils.hasText(username), SysOperLog::getUsername, username)
                .like(StringUtils.hasText(module), SysOperLog::getModule, module)
                .ge(beginTime != null, SysOperLog::getCreateTime, beginTime)
                .le(endTime != null, SysOperLog::getCreateTime, endTime)
                .orderByDesc(SysOperLog::getId);
        return PageResult.of(sysOperLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请先选择要删除的日志");
        }
        sysOperLogMapper.deleteBatchIds(ids);
    }
}
