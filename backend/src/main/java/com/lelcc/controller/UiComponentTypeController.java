package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.Result;
import com.lelcc.dto.ComponentTypeDTO;
import com.lelcc.dto.ComponentTypeVO;
import com.lelcc.service.UiComponentTypeService;
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

/** 组件库管理：一行一种可拖入用户端的组件。 */
@RestController
@RequestMapping("/admin/site/component-types")
public class UiComponentTypeController {

    @Resource
    private UiComponentTypeService componentTypeService;

    @GetMapping
    public Result<List<ComponentTypeVO>> list(@RequestParam(required = false) Integer status) {
        return Result.success(componentTypeService.list(status));
    }

    @OperLog(module = "组件库", operation = "新增组件")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ComponentTypeDTO dto) {
        componentTypeService.create(dto);
        return Result.success(null);
    }

    @OperLog(module = "组件库", operation = "修改组件")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ComponentTypeDTO dto) {
        componentTypeService.update(id, dto);
        return Result.success(null);
    }

    @OperLog(module = "组件库", operation = "删除组件")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        componentTypeService.delete(id);
        return Result.success(null);
    }
}
