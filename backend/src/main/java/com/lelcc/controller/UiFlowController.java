package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.Result;
import com.lelcc.dto.FlowDTO;
import com.lelcc.dto.FlowVO;
import com.lelcc.service.UiFlowService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 页面逻辑图：按 page + flowKey 整图保存。 */
@RestController
@RequestMapping("/admin/site/pages/{pageId}/flows")
public class UiFlowController {

    @Resource
    private UiFlowService flowService;

    @GetMapping
    public Result<List<FlowVO>> list(@PathVariable Long pageId) {
        return Result.success(flowService.listByPage(pageId));
    }

    @OperLog(module = "逻辑图", operation = "保存逻辑图")
    @PutMapping("/{flowKey}")
    public Result<Void> save(@PathVariable Long pageId,
                             @PathVariable String flowKey,
                             @Valid @RequestBody FlowDTO dto) {
        flowService.save(pageId, flowKey, dto);
        return Result.success(null);
    }
}
