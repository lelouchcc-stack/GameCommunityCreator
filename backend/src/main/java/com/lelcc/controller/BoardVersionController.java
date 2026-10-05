package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.Result;
import com.lelcc.dto.BoardVersionDTO;
import com.lelcc.dto.BoardVersionVO;
import com.lelcc.service.BoardVersionService;
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
@RequestMapping("/admin/board/{boardId}/versions")
public class BoardVersionController {

    @Resource
    private BoardVersionService boardVersionService;

    @GetMapping
    public Result<List<BoardVersionVO>> list(@PathVariable Long boardId) {
        return Result.success(boardVersionService.listByBoard(boardId));
    }

    @OperLog(module = "内容维度", operation = "新增版本")
    @PostMapping
    public Result<Void> create(@PathVariable Long boardId, @Valid @RequestBody BoardVersionDTO dto) {
        boardVersionService.create(boardId, dto);
        return Result.success(null);
    }

    @OperLog(module = "内容维度", operation = "修改版本")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long boardId, @PathVariable Long id,
                               @Valid @RequestBody BoardVersionDTO dto) {
        boardVersionService.update(boardId, id, dto);
        return Result.success(null);
    }

    @OperLog(module = "内容维度", operation = "设为当前版本")
    @PutMapping("/{id}/current")
    public Result<Void> setCurrent(@PathVariable Long boardId, @PathVariable Long id) {
        boardVersionService.setCurrent(boardId, id);
        return Result.success(null);
    }

    @OperLog(module = "内容维度", operation = "修改版本状态")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long boardId, @PathVariable Long id,
                                     @RequestParam Integer status) {
        boardVersionService.changeStatus(boardId, id, status);
        return Result.success(null);
    }

    @OperLog(module = "内容维度", operation = "删除版本")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long boardId, @PathVariable Long id) {
        boardVersionService.delete(boardId, id);
        return Result.success(null);
    }
}
