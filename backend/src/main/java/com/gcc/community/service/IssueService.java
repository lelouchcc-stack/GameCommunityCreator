package com.gcc.community.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcc.community.common.ApiException;
import com.gcc.community.common.PageResult;
import com.gcc.community.domain.IssueReport;
import com.gcc.community.dto.CreateIssueRequest;
import com.gcc.community.dto.IssueProgressView;
import com.gcc.community.dto.IssueReportView;
import com.gcc.community.dto.IssueSubmitResult;
import com.gcc.community.dto.IssueUpdateRequest;
import com.gcc.community.repository.IssueReportRepository;

/** 问题反馈的业务。 */
@Service
public class IssueService {

    public static final List<String> STATUSES = List.of("PENDING", "PROCESSING", "RESOLVED");
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final int MAX_RESOLUTION_LENGTH = 1000;
    public static final int MAX_PUBLIC_PROGRESS = 50;
    public static final int MAX_PROGRESS_DESCRIPTION_LENGTH = 500;

    @Autowired
    private ProjectService projectService;
    @Autowired
    private IssueReportRepository issueReportRepository;

    public PageResult<IssueReportView> list(long projectId, int pageNum, int pageSize, String status) {
        int safePageNum = PaginationSupport.requirePageNum(pageNum);
        int safePageSize = PaginationSupport.requirePageSize(pageSize);
        projectService.requireOwnedProject(projectId);
        String filter = normalizeStatusFilter(status);
        long total = issueReportRepository.count(projectId, filter);
        List<IssueReportView> items = issueReportRepository
                .findPage(projectId, filter, safePageSize,
                        PaginationSupport.offset(safePageNum, safePageSize)).stream()
                .map(this::toView).toList();
        return new PageResult<>(items, total, safePageNum, safePageSize);
    }

    // 公开提交，默认是 PENDING
    @Transactional
    public IssueSubmitResult submit(long projectId, CreateIssueRequest request) {
        projectService.requireProject(projectId);
        LocalDateTime now = LocalDateTime.now();
        long id = issueReportRepository.insert(projectId, request.title(), request.category(),
                request.description(), nullSafe(request.gameVersion()), nullSafe(request.steps()),
                STATUS_PENDING, now);
        return new IssueSubmitResult(id, STATUS_PENDING, now);
    }

    // 管理端更新。设成公开时状态必须是 RESOLVED，不然报 400
    @Transactional
    public IssueReportView update(long issueId, IssueUpdateRequest request) {
        IssueReport existing = requireIssue(issueId);
        projectService.requireOwnedProject(existing.projectId());

        Map<String, String> errors = new LinkedHashMap<>();
        String status = existing.status();
        if (request.status() != null) {
            if (!STATUSES.contains(request.status())) {
                errors.put("status", "status 只能是 PENDING、PROCESSING 或 RESOLVED");
            } else {
                status = request.status();
            }
        }
        String resolution = request.resolution() == null
                ? existing.resolution() : request.resolution();
        if (resolution != null && resolution.length() > MAX_RESOLUTION_LENGTH) {
            errors.put("resolution", "处理结果长度不能超过 1000 个字符");
        }
        boolean isPublic = request.isPublic() == null ? existing.isPublic() : request.isPublic();
        if (!errors.isEmpty()) {
            ContentValidation.throwIfAny("问题反馈参数校验失败", errors);
        }
        if (isPublic && !STATUS_RESOLVED.equals(status)) {
            throw new ApiException(400, "仅已解决的问题可公开展示",
                    Map.of("isPublic", "仅已解决的问题可公开展示"));
        }

        String safeResolution = resolution == null ? "" : resolution;
        LocalDateTime now = LocalDateTime.now();
        issueReportRepository.update(issueId, status, safeResolution, isPublic, now);
        return toView(new IssueReport(existing.id(), existing.projectId(), existing.title(),
                existing.category(), existing.description(), existing.gameVersion(), existing.steps(),
                status, safeResolution, isPublic, existing.createdAt(), now));
    }

    @Transactional
    public void delete(long issueId) {
        IssueReport existing = requireIssue(issueId);
        projectService.requireOwnedProject(existing.projectId());
        issueReportRepository.deleteById(issueId);
    }

    // 公开的进展列表，只取已解决且公开的，最多 50 条
    public List<IssueProgressView> publicProgress(long projectId) {
        return issueReportRepository.findPublicProgress(projectId, MAX_PUBLIC_PROGRESS).stream()
                .map(issue -> new IssueProgressView(issue.id(), issue.title(), issue.category(),
                        truncate(issue.description(), MAX_PROGRESS_DESCRIPTION_LENGTH),
                        nullSafe(issue.resolution()), issue.updatedAt()))
                .toList();
    }

    private IssueReport requireIssue(long issueId) {
        return issueReportRepository.findById(issueId)
                .orElseThrow(() -> ApiException.notFound("问题反馈不存在"));
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if (!STATUSES.contains(status)) {
            throw ApiException.badRequest("status 只能是 PENDING、PROCESSING 或 RESOLVED");
        }
        return status;
    }

    private IssueReportView toView(IssueReport issue) {
        return new IssueReportView(issue.id(), issue.title(), issue.category(), issue.description(),
                issue.gameVersion(), issue.steps(), issue.status(), issue.resolution(), issue.isPublic(),
                issue.createdAt(), issue.updatedAt());
    }

    // 按码点截断，别把 emoji 截坏了
    private static String truncate(String value, int maxCodePoints) {
        if (value == null) {
            return "";
        }
        if (value.codePointCount(0, value.length()) <= maxCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
