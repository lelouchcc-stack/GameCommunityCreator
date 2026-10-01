package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.PageResult;
import com.lelcc.common.Result;
import com.lelcc.dto.BoardDTO;
import com.lelcc.dto.BoardVO;
import com.lelcc.service.BoardService;
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

@RestController
@RequestMapping("/admin/board")
public class BoardController {

    @Resource
    private BoardService boardService;

    @GetMapping("/page")
    public Result<PageResult<BoardVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(boardService.page(name, status, pageNum, pageSize));
    }

    @OperLog(module = "板块管理", operation = "新增板块")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody BoardDTO dto) {
        boardService.create(dto);
        return Result.success(null);
    }

    @OperLog(module = "板块管理", operation = "修改板块")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BoardDTO dto) {
        boardService.update(id, dto);
        return Result.success(null);
    }

    /** 设为开放(1)或关闭(0)，不做物理删除 */
    @OperLog(module = "板块管理", operation = "修改板块状态")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        boardService.changeStatus(id, status);
        return Result.success(null);
    }

    @OperLog(module = "板块管理", operation = "删除板块")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boardService.delete(id);
        return Result.success(null);
    }
}
