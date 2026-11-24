package com.gcc.community.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gcc.community.common.ApiResponse;
import com.gcc.community.dto.NavigationEnvelope;
import com.gcc.community.dto.NavigationPayload;
import com.gcc.community.dto.SiteSettingsView;
import com.gcc.community.dto.UpdateSettingsRequest;
import com.gcc.community.service.NavigationService;
import com.gcc.community.service.SiteSettingsService;

import jakarta.validation.Valid;

/** 站点导航和设置的接口。 */
@RestController
@RequestMapping("/api/projects/{projectId}")
public class SiteConfigController {

    @Autowired
    private NavigationService navigationService;
    @Autowired
    private SiteSettingsService siteSettingsService;

    // 读导航
    @GetMapping("/navigation")
    public ApiResponse<NavigationEnvelope> getNavigation(@PathVariable long projectId) {
        return ApiResponse.ok(navigationService.get(projectId));
    }

    // 整体替换导航
    @PutMapping("/navigation")
    public ApiResponse<NavigationEnvelope> saveNavigation(@PathVariable long projectId,
            @RequestBody NavigationPayload payload) {
        return ApiResponse.ok(navigationService.save(projectId, payload));
    }

    // 读站点设置
    @GetMapping("/settings")
    public ApiResponse<SiteSettingsView> getSettings(@PathVariable long projectId) {
        return ApiResponse.ok(siteSettingsService.get(projectId));
    }

    // 主题模式只能是 DARK、LIGHT 或 SYSTEM
    @PutMapping("/settings")
    public ApiResponse<SiteSettingsView> saveSettings(@PathVariable long projectId,
            @Valid @RequestBody UpdateSettingsRequest request) {
        return ApiResponse.ok(siteSettingsService.save(projectId, request));
    }
}
