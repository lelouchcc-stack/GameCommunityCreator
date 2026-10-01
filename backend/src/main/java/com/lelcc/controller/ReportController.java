package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.PageResult;
import com.lelcc.common.Result;
import com.lelcc.dto.ReportHandleDTO;
import com.lelcc.dto.ReportVO;
import com.lelcc.service.ReportService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/report")
public class ReportController {

    @Resource
    private ReportService reportService;

    @GetMapping("/page")
    public Result<PageResult<ReportVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) Integer status) {
        return Result.success(reportService.page(status, pageNum, pageSize));
    }

    @OperLog(module = "举报处理", operation = "处理举报")
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody ReportHandleDTO dto) {
        reportService.handle(id, dto.status(), dto.remark(), dto.offlinePost());
        return Result.success(null);
    }
}
