package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.PageResult;
import com.lelcc.common.Result;
import com.lelcc.dto.TagDTO;
import com.lelcc.dto.TagVO;
import com.lelcc.service.TagService;
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
@RequestMapping("/admin/tag")
public class TagController {

    @Resource
    private TagService tagService;

    @GetMapping("/page")
    public Result<PageResult<TagVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) Long boardId,
                                          @RequestParam(required = false) Integer tagType,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) String keyword) {
        return Result.success(tagService.page(boardId, tagType, status, keyword, pageNum, pageSize));
    }

    @OperLog(module = "标签管理", operation = "新增标签")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody TagDTO dto) {
        tagService.create(dto);
        return Result.success(null);
    }

    @OperLog(module = "标签管理", operation = "修改标签")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TagDTO dto) {
        tagService.update(id, dto);
        return Result.success(null);
    }

    @OperLog(module = "标签管理", operation = "修改标签状态")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        tagService.changeStatus(id, status);
        return Result.success(null);
    }

    @OperLog(module = "标签管理", operation = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.success(null);
    }
}
