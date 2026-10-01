package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.Result;
import com.lelcc.dto.HomeModuleDTO;
import com.lelcc.dto.HomeModuleVO;
import com.lelcc.dto.SiteConfigDTO;
import com.lelcc.dto.SiteConfigVO;
import com.lelcc.service.SiteService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/site")
public class SiteConfigController {

    @Resource
    private SiteService siteService;

    @GetMapping("/config")
    public Result<SiteConfigVO> getConfig() {
        return Result.success(siteService.getConfig());
    }

    /** 返回保存后的最新配置，管理端据此就地预览新主题 */
    @OperLog(module = "站点配置", operation = "修改站点信息")
    @PutMapping("/config")
    public Result<SiteConfigVO> updateConfig(@RequestBody SiteConfigDTO dto) {
        return Result.success(siteService.updateConfig(dto));
    }

    @GetMapping("/home-modules")
    public Result<List<HomeModuleVO>> listModules() {
        return Result.success(siteService.listModules());
    }

    /** 整表批量保存：启用状态与排序一次提交，避免拖拽排序产生多条写请求 */
    @OperLog(module = "站点配置", operation = "修改首页模块")
    @PutMapping("/home-modules")
    public Result<List<HomeModuleVO>> saveModules(@RequestBody List<HomeModuleDTO> modules) {
        return Result.success(siteService.saveModules(modules));
    }
}
