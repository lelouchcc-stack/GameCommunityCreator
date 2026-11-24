package com.gcc.community.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcc.community.common.ApiException;
import com.gcc.community.common.PageResult;
import com.gcc.community.domain.Changelog;
import com.gcc.community.dto.ChangelogRequest;
import com.gcc.community.dto.ChangelogView;
import com.gcc.community.repository.ChangelogRepository;

/** 更新日志的业务。 */
@Service
public class ChangelogService {

    private static final List<String> STATUSES = List.of("DRAFT", "PUBLISHED");

    @Autowired
    private ProjectService projectService;
    @Autowired
    private ChangelogRepository changelogRepository;

    public PageResult<ChangelogView> list(long projectId, int pageNum, int pageSize, String status) {
        projectService.requireOwnedProject(projectId);
        int safePageNum = PaginationSupport.requirePageNum(pageNum);
        int safePageSize = PaginationSupport.requirePageSize(pageSize);
        String filter = normalizeStatusFilter(status);
        long total = changelogRepository.count(projectId, filter);
        List<ChangelogView> items = changelogRepository.findPage(projectId, filter, safePageSize,
                        PaginationSupport.offset(safePageNum, safePageSize)).stream()
                .map(this::toView).toList();
        return new PageResult<>(items, total, safePageNum, safePageSize);
    }

    @Transactional
    public ChangelogView create(long projectId, ChangelogRequest request) {
        projectService.requireOwnedProject(projectId);
        Map<String, String> errors = new LinkedHashMap<>();
        String version = request.version();
        String title = request.title();
        String markdown = firstNonBlank(request.bodyMarkdown(), request.content());
        LocalDate releaseDate = request.releaseDate();
        String status = request.status() == null || request.status().isBlank() ? "DRAFT" : request.status();

        validate(errors, version, title, markdown, releaseDate, status);
        ContentValidation.throwIfAny("更新日志参数校验失败", errors);

        LocalDateTime now = LocalDateTime.now();
        long id = changelogRepository.insert(projectId, version, title, markdown, releaseDate, status, now);
        return toView(new Changelog(id, projectId, version, title, markdown, markdown, releaseDate, status,
                now, now));
    }

    @Transactional
    public ChangelogView update(long entryId, ChangelogRequest request) {
        Changelog existing = requireEntry(entryId);
        projectService.requireOwnedProject(existing.projectId());
        Map<String, String> errors = new LinkedHashMap<>();
        String version = request.version() == null ? existing.version() : request.version();
        String title = request.title() == null ? existing.title() : request.title();
        String markdown = firstNonBlank(request.bodyMarkdown(), request.content());
        if (markdown == null) {
            markdown = existing.bodyMarkdown();
        }
        LocalDate releaseDate = request.releaseDate() == null ? existing.releaseDate() : request.releaseDate();
        String status = request.status() == null || request.status().isBlank()
                ? existing.status() : request.status();

        validate(errors, version, title, markdown, releaseDate, status);
        ContentValidation.throwIfAny("更新日志参数校验失败", errors);

        LocalDateTime now = LocalDateTime.now();
        changelogRepository.update(entryId, version, title, markdown, releaseDate, status, now);
        return toView(new Changelog(entryId, existing.projectId(), version, title, markdown, markdown,
                releaseDate, status, existing.createdAt(), now));
    }

    @Transactional
    public void delete(long entryId) {
        Changelog existing = requireEntry(entryId);
        projectService.requireOwnedProject(existing.projectId());
        changelogRepository.deleteById(entryId);
    }

    // 公开端

    public PageResult<ChangelogView> publicList(long projectId, int pageNum, int pageSize) {
        int safePageNum = PaginationSupport.requirePageNum(pageNum);
        int safePageSize = PaginationSupport.requirePageSize(pageSize);
        long total = changelogRepository.count(projectId, "PUBLISHED");
        List<ChangelogView> items = changelogRepository
                .findPage(projectId, "PUBLISHED", safePageSize,
                        PaginationSupport.offset(safePageNum, safePageSize)).stream()
                .map(this::toView).toList();
        return new PageResult<>(items, total, safePageNum, safePageSize);
    }

    public ChangelogView publicDetail(long projectId, long entryId) {
        Changelog entry = changelogRepository.findById(entryId)
                .filter(row -> row.projectId() == projectId && "PUBLISHED".equals(row.status()))
                .orElseThrow(() -> ApiException.notFound("该更新日志不存在或尚未发布"));
        return toView(entry);
    }

    private Changelog requireEntry(long entryId) {
        return changelogRepository.findById(entryId)
                .orElseThrow(() -> ApiException.notFound("更新日志不存在"));
    }

    private void validate(Map<String, String> errors, String version, String title, String markdown,
            LocalDate releaseDate, String status) {
        ContentValidation.requiredText(errors, "version", version, 40);
        ContentValidation.requiredText(errors, "title", title, 120);
        if (markdown == null || markdown.strip().isEmpty()) {
            errors.put("bodyMarkdown", "正文不能为空");
        } else if (markdown.length() > 10000) {
            errors.put("bodyMarkdown", "正文长度不能超过 10000 个字符");
        }
        if (releaseDate == null) {
            errors.put("releaseDate", "不能为空");
        }
        ContentValidation.requiredEnum(errors, "status", status, STATUSES);
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if (!STATUSES.contains(status)) {
            throw ApiException.badRequest("status 只能是 DRAFT 或 PUBLISHED");
        }
        return status;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private ChangelogView toView(Changelog entry) {
        return new ChangelogView(entry.id(), entry.version(), entry.title(), entry.bodyMarkdown(),
                entry.content(), entry.releaseDate(), entry.status(), entry.createdAt(), entry.updatedAt());
    }
}
