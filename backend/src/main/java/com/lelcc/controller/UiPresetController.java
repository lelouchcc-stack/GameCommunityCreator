package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.Result;
import com.lelcc.dto.PresetDTO;
import com.lelcc.dto.PresetVO;
import com.lelcc.service.UiPresetService;
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

/** 预设库（原子团）：内置只读、可复制为自定义；自定义可增删改，被引用时改动产生新版本。 */
@RestController
@RequestMapping("/admin/site/presets")
public class UiPresetController {

    @Resource
    private UiPresetService presetService;

    @GetMapping
    public Result<List<PresetVO>> list(@RequestParam(required = false) Integer status) {
        return Result.success(presetService.list(status));
    }

    @OperLog(module = "预设库", operation = "新增预设")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PresetDTO dto) {
        presetService.create(dto);
        return Result.success(null);
    }

    @OperLog(module = "预设库", operation = "修改预设")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PresetDTO dto) {
        presetService.update(id, dto);
        return Result.success(null);
    }

    @OperLog(module = "预设库", operation = "删除预设")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        presetService.delete(id);
        return Result.success(null);
    }
}
