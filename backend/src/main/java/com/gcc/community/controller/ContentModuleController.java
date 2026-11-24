package com.gcc.community.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gcc.community.common.ApiResponse;
import com.gcc.community.common.PageResult;
import com.gcc.community.dto.ChangelogRequest;
import com.gcc.community.dto.ChangelogView;
import com.gcc.community.dto.DocRequest;
import com.gcc.community.dto.DocView;
import com.gcc.community.dto.FriendLinkRequest;
import com.gcc.community.dto.FriendLinkView;
import com.gcc.community.dto.IssueReportView;
import com.gcc.community.dto.IssueUpdateRequest;
import com.gcc.community.dto.TeamMemberRequest;
import com.gcc.community.dto.TeamMemberView;
import com.gcc.community.service.ChangelogService;
import com.gcc.community.service.DocService;
import com.gcc.community.service.FriendLinkService;
import com.gcc.community.service.IssueService;
import com.gcc.community.service.TeamMemberService;

import jakarta.validation.Valid;

/** 更新日志、文档、团队成员、问题反馈、友情链接的管理接口。 */
@RestController
@RequestMapping("/api")
public class ContentModuleController {
    @Autowired
    private ChangelogService changelogService;
    @Autowired
    private DocService docService;
    @Autowired
    private TeamMemberService teamMemberService;
    @Autowired
    private IssueService issueService;
    @Autowired
    private FriendLinkService friendLinkService;

    // 更新日志

    @GetMapping("/projects/{projectId}/changelogs")
    public ApiResponse<PageResult<ChangelogView>> listChangelogs(@PathVariable long projectId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(changelogService.list(projectId, pageNum, pageSize, status));
    }

    @PostMapping("/projects/{projectId}/changelogs")
    public ResponseEntity<ApiResponse<ChangelogView>> createChangelog(@PathVariable long projectId,
            @RequestBody ChangelogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(changelogService.create(projectId, request)));
    }

    @PutMapping("/changelogs/{entryId}")
    public ApiResponse<ChangelogView> updateChangelog(@PathVariable long entryId,
            @RequestBody ChangelogRequest request) {
        return ApiResponse.ok(changelogService.update(entryId, request));
    }

    @DeleteMapping("/changelogs/{entryId}")
    public ResponseEntity<Void> deleteChangelog(@PathVariable long entryId) {
        changelogService.delete(entryId);
        return ResponseEntity.noContent().build();
    }

    // 公开文档

    @GetMapping("/projects/{projectId}/docs")
    public ApiResponse<PageResult<DocView>> listDocs(@PathVariable long projectId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        return ApiResponse.ok(docService.list(projectId, pageNum, pageSize, status, category));
    }

    @PostMapping("/projects/{projectId}/docs")
    public ResponseEntity<ApiResponse<DocView>> createDoc(@PathVariable long projectId,
            @RequestBody DocRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(docService.create(projectId, request)));
    }

    @PutMapping("/docs/{docId}")
    public ApiResponse<DocView> updateDoc(@PathVariable long docId, @RequestBody DocRequest request) {
        return ApiResponse.ok(docService.update(docId, request));
    }

    @DeleteMapping("/docs/{docId}")
    public ResponseEntity<Void> deleteDoc(@PathVariable long docId) {
        docService.delete(docId);
        return ResponseEntity.noContent().build();
    }

    // 制作组成员

    @GetMapping("/projects/{projectId}/team-members")
    public ApiResponse<List<TeamMemberView>> listTeamMembers(@PathVariable long projectId) {
        return ApiResponse.ok(teamMemberService.list(projectId));
    }

    @PostMapping("/projects/{projectId}/team-members")
    public ResponseEntity<ApiResponse<TeamMemberView>> createTeamMember(@PathVariable long projectId,
            @RequestBody TeamMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(teamMemberService.create(projectId, request)));
    }

    @PutMapping("/team-members/{memberId}")
    public ApiResponse<TeamMemberView> updateTeamMember(@PathVariable long memberId,
            @RequestBody TeamMemberRequest request) {
        return ApiResponse.ok(teamMemberService.update(memberId, request));
    }

    @DeleteMapping("/team-members/{memberId}")
    public ResponseEntity<Void> deleteTeamMember(@PathVariable long memberId) {
        teamMemberService.delete(memberId);
        return ResponseEntity.noContent().build();
    }

    // 问题反馈

    @GetMapping("/projects/{projectId}/issues")
    public ApiResponse<PageResult<IssueReportView>> listIssues(@PathVariable long projectId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(issueService.list(projectId, pageNum, pageSize, status));
    }

    // 改问题的状态或者处理结果
    @PatchMapping("/issues/{issueId}")
    public ApiResponse<IssueReportView> updateIssue(@PathVariable long issueId,
            @RequestBody IssueUpdateRequest request) {
        return ApiResponse.ok(issueService.update(issueId, request));
    }

    @DeleteMapping("/issues/{issueId}")
    public ResponseEntity<Void> deleteIssue(@PathVariable long issueId) {
        issueService.delete(issueId);
        return ResponseEntity.noContent().build();
    }

    // 友情链接

    @GetMapping("/projects/{projectId}/links")
    public ApiResponse<List<FriendLinkView>> listLinks(@PathVariable long projectId) {
        return ApiResponse.ok(friendLinkService.list(projectId));
    }

    @PostMapping("/projects/{projectId}/links")
    public ResponseEntity<ApiResponse<FriendLinkView>> createLink(@PathVariable long projectId,
            @RequestBody FriendLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(friendLinkService.create(projectId, request)));
    }

    @PutMapping("/links/{linkId}")
    public ApiResponse<FriendLinkView> updateLink(@PathVariable long linkId,
            @RequestBody FriendLinkRequest request) {
        return ApiResponse.ok(friendLinkService.update(linkId, request));
    }

    @DeleteMapping("/links/{linkId}")
    public ResponseEntity<Void> deleteLink(@PathVariable long linkId) {
        friendLinkService.delete(linkId);
        return ResponseEntity.noContent().build();
    }
}
