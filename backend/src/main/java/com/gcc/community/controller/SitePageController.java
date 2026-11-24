package com.gcc.community.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gcc.community.common.ApiResponse;
import com.gcc.community.dto.CreatePageRequest;
import com.gcc.community.dto.PageEnvelopeV2;
import com.gcc.community.dto.PublicationView;
import com.gcc.community.dto.PublishPageResult;
import com.gcc.community.dto.SavePageRequest;
import com.gcc.community.dto.SitePageView;
import com.gcc.community.dto.SitePublishResult;
import com.gcc.community.dto.UpdatePageRequest;
import com.gcc.community.service.PageConfigService;
import com.gcc.community.service.SitePageService;

import jakarta.validation.Valid;

/** 页面管理的接口。 */
@RestController
@RequestMapping("/api/projects/{projectId}")
public class SitePageController {

    @Autowired
    private SitePageService sitePageService;
    @Autowired
    private PageConfigService pageConfigService;

    // 页面列表
    @GetMapping("/pages")
    public ApiResponse<List<SitePageView>> list(@PathVariable long projectId) {
        return ApiResponse.ok(sitePageService.list(projectId));
    }

    // 新建页面
    @PostMapping("/pages")
    public ResponseEntity<ApiResponse<SitePageView>> create(@PathVariable long projectId,
            @Valid @RequestBody CreatePageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(sitePageService.create(projectId, request)));
    }

    @GetMapping("/pages/{pageId}")
    public ApiResponse<SitePageView> get(@PathVariable long projectId, @PathVariable String pageId) {
        return ApiResponse.ok(sitePageService.get(projectId, pageId));
    }

    // 修改页面信息
    @PutMapping("/pages/{pageId}")
    public ApiResponse<SitePageView> update(@PathVariable long projectId, @PathVariable String pageId,
            @RequestBody UpdatePageRequest request) {
        return ApiResponse.ok(sitePageService.update(projectId, pageId, request));
    }

    @DeleteMapping("/pages/{pageId}")
    public ResponseEntity<Void> delete(@PathVariable long projectId, @PathVariable String pageId) {
        sitePageService.delete(projectId, pageId);
        return ResponseEntity.noContent().build();
    }

    // 复制页面
    @PostMapping("/pages/{pageId}/copy")
    public ResponseEntity<ApiResponse<SitePageView>> copy(@PathVariable long projectId,
            @PathVariable String pageId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(sitePageService.copy(projectId, pageId)));
    }

    // 读取页面草稿
    @GetMapping("/pages/{pageId}/page")
    public ApiResponse<PageEnvelopeV2> getDraft(@PathVariable long projectId,
            @PathVariable String pageId) {
        return ApiResponse.ok(pageConfigService.getPageDraft(projectId, pageId));
    }

    // 保存草稿，版本对不上就冲突
    @PutMapping("/pages/{pageId}/page")
    public ApiResponse<PageEnvelopeV2> saveDraft(@PathVariable long projectId,
            @PathVariable String pageId, @Valid @RequestBody SavePageRequest request) {
        return ApiResponse.ok(pageConfigService.savePageDraft(projectId, pageId, request));
    }

    // 按模板重置草稿
    @PostMapping("/pages/{pageId}/page/reset-default")
    public ApiResponse<PageEnvelopeV2> resetDefault(@PathVariable long projectId,
            @PathVariable String pageId) {
        return ApiResponse.ok(pageConfigService.resetPageDraft(projectId, pageId));
    }

    // 发布这个页面
    @PostMapping("/pages/{pageId}/publish")
    public ApiResponse<PublishPageResult> publish(@PathVariable long projectId,
            @PathVariable String pageId) {
        return ApiResponse.ok(sitePageService.publishPage(projectId, pageId));
    }

    // 发布历史
    @GetMapping("/pages/{pageId}/publications")
    public ApiResponse<List<PublicationView>> publications(@PathVariable long projectId,
            @PathVariable String pageId) {
        return ApiResponse.ok(sitePageService.listPublications(projectId, pageId));
    }

    // 整站发布
    @PostMapping("/publish-site")
    public ApiResponse<SitePublishResult> publishSite(@PathVariable long projectId) {
        return ApiResponse.ok(sitePageService.publishSite(projectId));
    }
}
