package com.lelcc.controller;

import com.lelcc.common.PageResult;
import com.lelcc.common.Result;
import com.lelcc.entity.SysOperLog;
import com.lelcc.service.SysOperLogService;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/log")
public class SysOperLogController {

    @Resource
    private SysOperLogService sysOperLogService;

    @GetMapping("/page")
    public Result<PageResult<SysOperLog>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beginTime,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.success(
                sysOperLogService.page(username, module, beginTime, endTime, pageNum, pageSize));
    }

    /** 路径上用逗号分隔多个 id，例如 /admin/log/3,4,5 */
    @DeleteMapping("/{ids}")
    public Result<Void> deleteBatch(@PathVariable List<Long> ids) {
        sysOperLogService.deleteBatch(ids);
        return Result.success(null);
    }
}
