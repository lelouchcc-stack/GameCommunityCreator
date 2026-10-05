package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.Result;
import com.lelcc.dto.ProgressStageDTO;
import com.lelcc.dto.ProgressStageVO;
import com.lelcc.dto.StageSortDTO;
import com.lelcc.service.ProgressStageService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/board/{boardId}/stages")
public class ProgressStageController {

    @Resource
    private ProgressStageService progressStageService;

    @GetMapping
    public Result<List<ProgressStageVO>> list(@PathVariable Long boardId) {
        return Result.success(progressStageService.listByBoard(boardId));
    }

    @OperLog(module = "内容维度", operation = "新增进度阶段")
    @PostMapping
    public Result<Void> create(@PathVariable Long boardId, @Valid @RequestBody ProgressStageDTO dto) {
        progressStageService.create(boardId, dto);
        return Result.success(null);
    }

    @OperLog(module = "内容维度", operation = "修改进度阶段")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long boardId, @PathVariable Long id,
                               @Valid @RequestBody ProgressStageDTO dto) {
        progressStageService.update(boardId, id, dto);
        return Result.success(null);
    }

    @OperLog(module = "内容维度", operation = "调整进度阶段顺序")
    @PutMapping("/sort")
    public Result<Void> reorder(@PathVariable Long boardId,
                                @Valid @RequestBody List<StageSortDTO> items) {
        progressStageService.reorder(boardId, items);
        return Result.success(null);
    }

    @OperLog(module = "内容维度", operation = "修改进度阶段状态")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long boardId, @PathVariable Long id,
                                     @RequestParam Integer status) {
        progressStageService.changeStatus(boardId, id, status);
        return Result.success(null);
    }

    @OperLog(module = "内容维度", operation = "删除进度阶段")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long boardId, @PathVariable Long id) {
        progressStageService.delete(boardId, id);
        return Result.success(null);
    }
}
