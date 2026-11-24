package com.gcc.community.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gcc.community.common.ApiResponse;
import com.gcc.community.dto.PageEnvelope;
import com.gcc.community.dto.PublicationView;
import com.gcc.community.dto.PublishResult;
import com.gcc.community.dto.SavePageRequest;
import com.gcc.community.service.PageConfigService;

import jakarta.validation.Valid;

/** 旧版单页接口，其实都指向 home 页面。 */
@RestController
@RequestMapping("/api/projects")
public class PageConfigController {

    @Autowired
    private PageConfigService pageConfigService;

    // 读取 home 页草稿
    @GetMapping("/{id}/page")
    public ApiResponse<PageEnvelope> getDraft(@PathVariable long id) {
        return ApiResponse.ok(pageConfigService.getHomeDraft(id));
    }

    // 保存 home 页草稿，带乐观锁
    @PutMapping("/{id}/page")
    public ApiResponse<PageEnvelope> saveDraft(@PathVariable long id,
            @Valid @RequestBody SavePageRequest request) {
        return ApiResponse.ok(pageConfigService.saveHomeDraft(id, request));
    }

    // 重置成默认模板
    @PostMapping("/{id}/page/reset-default")
    public ApiResponse<PageEnvelope> resetDefault(@PathVariable long id) {
        return ApiResponse.ok(pageConfigService.resetHomeDraft(id));
    }

    // 发布 home 页，返回旧结构
    @PostMapping("/{id}/publish")
    public ApiResponse<PublishResult> publish(@PathVariable long id) {
        return ApiResponse.ok(pageConfigService.publishHome(id));
    }

    // 发布历史
    @GetMapping("/{id}/publications")
    public ApiResponse<List<PublicationView>> publications(@PathVariable long id) {
        return ApiResponse.ok(pageConfigService.listHomePublications(id));
    }
}
