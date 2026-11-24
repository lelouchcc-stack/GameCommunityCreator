package com.gcc.community.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gcc.community.common.ApiResponse;
import com.gcc.community.common.PageResult;
import com.gcc.community.domain.Announcement;
import com.gcc.community.domain.GameInfo;
import com.gcc.community.dto.ChangelogView;
import com.gcc.community.dto.CreateIssueRequest;
import com.gcc.community.dto.DocView;
import com.gcc.community.dto.FriendLinkView;
import com.gcc.community.dto.IssueProgressView;
import com.gcc.community.dto.IssueSubmitResult;
import com.gcc.community.dto.PublicCommunityDetail;
import com.gcc.community.dto.PublicCommunitySummary;
import com.gcc.community.dto.TeamMemberView;
import com.gcc.community.service.PublicCommunityService;

import jakarta.validation.Valid;

/** 公开端的接口，只能看已经发布的内容。 */
@RestController
@RequestMapping("/api/public/communities")
public class PublicCommunityController {

    @Autowired
    private PublicCommunityService publicCommunityService;

    // 已发布的站点列表
    @GetMapping
    public ApiResponse<List<PublicCommunitySummary>> list() {
        return ApiResponse.ok(publicCommunityService.listCommunities());
    }

    // 站点首页和上下文
    @GetMapping("/{slug}")
    public ApiResponse<PublicCommunityDetail> detail(@PathVariable String slug) {
        return ApiResponse.ok(publicCommunityService.getCommunity(slug));
    }

    // 按 slug 取指定页面
    @GetMapping("/{slug}/pages/{pageSlug}")
    public ApiResponse<PublicCommunityDetail> page(@PathVariable String slug,
            @PathVariable String pageSlug) {
        return ApiResponse.ok(publicCommunityService.getPage(slug, pageSlug));
    }

    @GetMapping("/{slug}/game-info")
    public ApiResponse<GameInfo> gameInfo(@PathVariable String slug) {
        return ApiResponse.ok(publicCommunityService.getGameInfo(slug));
    }

    @GetMapping("/{slug}/announcements")
    public ApiResponse<PageResult<Announcement>> announcements(@PathVariable String slug,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(publicCommunityService.listAnnouncements(slug, pageNum, pageSize));
    }

    // 更新日志，只要已发布的
    @GetMapping("/{slug}/changelogs")
    public ApiResponse<PageResult<ChangelogView>> changelogs(@PathVariable String slug,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(publicCommunityService.listChangelogs(slug, pageNum, pageSize));
    }

    @GetMapping("/{slug}/changelogs/{entryId}")
    public ApiResponse<ChangelogView> changelog(@PathVariable String slug, @PathVariable long entryId) {
        return ApiResponse.ok(publicCommunityService.getChangelog(slug, entryId));
    }

    // 文档列表，可以按分类过滤
    @GetMapping("/{slug}/docs")
    public ApiResponse<PageResult<DocView>> docs(@PathVariable String slug,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String category) {
        return ApiResponse.ok(publicCommunityService.listDocs(slug, pageNum, pageSize, category));
    }

    @GetMapping("/{slug}/docs/{docSlug}")
    public ApiResponse<DocView> doc(@PathVariable String slug, @PathVariable String docSlug) {
        return ApiResponse.ok(publicCommunityService.getDoc(slug, docSlug));
    }

    @GetMapping("/{slug}/team")
    public ApiResponse<List<TeamMemberView>> team(@PathVariable String slug) {
        return ApiResponse.ok(publicCommunityService.listTeam(slug));
    }

    // 友情链接，只要启用的
    @GetMapping("/{slug}/links")
    public ApiResponse<List<FriendLinkView>> links(@PathVariable String slug) {
        return ApiResponse.ok(publicCommunityService.listLinks(slug));
    }

    // 公开的问题处理进展
    @GetMapping("/{slug}/issues/progress")
    public ApiResponse<List<IssueProgressView>> issueProgress(@PathVariable String slug) {
        return ApiResponse.ok(publicCommunityService.listIssueProgress(slug));
    }

    // 公告详情
    @GetMapping("/{slug}/news/{announcementId}")
    public ApiResponse<Announcement> news(@PathVariable String slug,
            @PathVariable long announcementId) {
        return ApiResponse.ok(publicCommunityService.getNews(slug, announcementId));
    }

    // 公开提交问题反馈
    @PostMapping("/{slug}/issues")
    public ResponseEntity<ApiResponse<IssueSubmitResult>> submitIssue(@PathVariable String slug,
            @Valid @RequestBody CreateIssueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(publicCommunityService.submitIssue(slug, request)));
    }
}
