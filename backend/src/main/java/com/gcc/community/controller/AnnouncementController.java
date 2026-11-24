package com.gcc.community.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gcc.community.common.ApiResponse;
import com.gcc.community.common.PageResult;
import com.gcc.community.domain.Announcement;
import com.gcc.community.dto.CreateAnnouncementRequest;
import com.gcc.community.dto.UpdateAnnouncementRequest;
import com.gcc.community.service.AnnouncementService;

import jakarta.validation.Valid;

/** 公告管理接口。 */
@RestController
@RequestMapping("/api")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    // 公告列表，置顶的排前面
    @GetMapping("/projects/{projectId}/announcements")
    public ApiResponse<PageResult<Announcement>> list(@PathVariable long projectId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(announcementService.listForAdmin(projectId, pageNum, pageSize));
    }

    // 新建公告
    @PostMapping("/projects/{projectId}/announcements")
    public ResponseEntity<ApiResponse<Announcement>> create(@PathVariable long projectId,
            @Valid @RequestBody CreateAnnouncementRequest request) {
        Announcement announcement = announcementService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(announcement));
    }

    // 修改公告，字段可以只传一部分
    @PutMapping("/announcements/{announcementId}")
    public ApiResponse<Announcement> update(@PathVariable long announcementId,
            @RequestBody UpdateAnnouncementRequest request) {
        return ApiResponse.ok(announcementService.update(announcementId, request));
    }

    @DeleteMapping("/announcements/{announcementId}")
    public ResponseEntity<Void> delete(@PathVariable long announcementId) {
        announcementService.delete(announcementId);
        return ResponseEntity.noContent().build();
    }
}
