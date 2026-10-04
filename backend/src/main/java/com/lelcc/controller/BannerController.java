package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.Result;
import com.lelcc.dto.BannerSlideDTO;
import com.lelcc.dto.BannerSlideVO;
import com.lelcc.service.BannerSlideService;
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

/** 后台首页轮播图维护。数量有限，列表不分页，一次取全。 */
@RestController
@RequestMapping("/admin/banner")
public class BannerController {

    @Resource
    private BannerSlideService bannerSlideService;

    @GetMapping("/list")
    public Result<List<BannerSlideVO>> list() {
        return Result.success(bannerSlideService.listAll());
    }

    @OperLog(module = "站点配置", operation = "新增轮播图")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody BannerSlideDTO dto) {
        bannerSlideService.create(dto);
        return Result.success(null);
    }

    @OperLog(module = "站点配置", operation = "修改轮播图")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BannerSlideDTO dto) {
        bannerSlideService.update(id, dto);
        return Result.success(null);
    }

    /** 设为展示(1)或隐藏(0) */
    @OperLog(module = "站点配置", operation = "修改轮播图状态")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        bannerSlideService.changeStatus(id, status);
        return Result.success(null);
    }

    /** 整组重排序：body 是轮播图 id 的展示顺序 */
    @OperLog(module = "站点配置", operation = "调整轮播图顺序")
    @PutMapping("/sort")
    public Result<Void> reorder(@RequestBody List<Long> ids) {
        bannerSlideService.reorder(ids);
        return Result.success(null);
    }

    @OperLog(module = "站点配置", operation = "删除轮播图")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerSlideService.delete(id);
        return Result.success(null);
    }
}
