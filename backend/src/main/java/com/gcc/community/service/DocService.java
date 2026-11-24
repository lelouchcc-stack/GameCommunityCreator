package com.gcc.community.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcc.community.common.ApiException;
import com.gcc.community.common.PageResult;
import com.gcc.community.domain.DocArticle;
import com.gcc.community.dto.DocRequest;
import com.gcc.community.dto.DocView;
import com.gcc.community.repository.DocArticleRepository;

/** 公开文档的业务。 */
@Service
public class DocService {

    private static final List<String> STATUSES = List.of("DRAFT", "PUBLISHED");
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{0,62}$");

    @Autowired
    private ProjectService projectService;
    @Autowired
    private DocArticleRepository docArticleRepository;

    public PageResult<DocView> list(long projectId, int pageNum, int pageSize, String status, String category) {
        projectService.requireOwnedProject(projectId);
        int safePageNum = PaginationSupport.requirePageNum(pageNum);
        int safePageSize = PaginationSupport.requirePageSize(pageSize);
        String filter = normalizeStatusFilter(status);
        String categoryFilter = category == null || category.isBlank() ? null : category.strip();
        long total = docArticleRepository.count(projectId, filter, categoryFilter);
        List<DocView> items = docArticleRepository
                .findPage(projectId, filter, categoryFilter, safePageSize,
                        PaginationSupport.offset(safePageNum, safePageSize)).stream()
                .map(this::toView).toList();
        return new PageResult<>(items, total, safePageNum, safePageSize);
    }

    @Transactional
    public DocView create(long projectId, DocRequest request) {
        projectService.requireOwnedProject(projectId);
        Map<String, String> errors = new LinkedHashMap<>();
        String title = request.title();
        String category = request.category() == null ? "" : request.category();
        String markdown = firstNonBlank(request.bodyMarkdown(), request.content());
        String slug = request.slug();
        String status = request.status() == null || request.status().isBlank() ? "DRAFT" : request.status();

        validate(errors, title, category, markdown, slug, status);
        ContentValidation.throwIfAny("文档参数校验失败", errors);
        if (docArticleRepository.existsSlug(projectId, slug, null)) {
            throw docSlugConflict();
        }

        LocalDateTime now = LocalDateTime.now();
        long id;
        try {
            id = docArticleRepository.insert(projectId, title, category, markdown, slug, status, now);
        } catch (DuplicateKeyException ex) {
            throw docSlugConflict();
        }
        return toView(new DocArticle(id, projectId, title, category, markdown, markdown, slug, status,
                now, now));
    }

    @Transactional
    public DocView update(long docId, DocRequest request) {
        DocArticle existing = requireDoc(docId);
        projectService.requireOwnedProject(existing.projectId());
        Map<String, String> errors = new LinkedHashMap<>();
        String title = request.title() == null ? existing.title() : request.title();
        String category = request.category() == null ? existing.category() : request.category();
        String markdown = firstNonBlank(request.bodyMarkdown(), request.content());
        if (markdown == null) {
            markdown = existing.bodyMarkdown();
        }
        String slug = request.slug() == null ? existing.slug() : request.slug();
        String status = request.status() == null || request.status().isBlank()
                ? existing.status() : request.status();

        validate(errors, title, category, markdown, slug, status);
        ContentValidation.throwIfAny("文档参数校验失败", errors);
        if (docArticleRepository.existsSlug(existing.projectId(), slug, docId)) {
            throw docSlugConflict();
        }

        LocalDateTime now = LocalDateTime.now();
        try {
            docArticleRepository.update(docId, title, category, markdown, slug, status, now);
        } catch (DuplicateKeyException ex) {
            throw docSlugConflict();
        }
        return toView(new DocArticle(docId, existing.projectId(), title, category, markdown, markdown,
                slug, status, existing.createdAt(), now));
    }

    @Transactional
    public void delete(long docId) {
        DocArticle existing = requireDoc(docId);
        projectService.requireOwnedProject(existing.projectId());
        docArticleRepository.deleteById(docId);
    }

    // 公开端

    public PageResult<DocView> publicList(long projectId, int pageNum, int pageSize, String category) {
        int safePageNum = PaginationSupport.requirePageNum(pageNum);
        int safePageSize = PaginationSupport.requirePageSize(pageSize);
        String categoryFilter = category == null || category.isBlank() ? null : category.strip();
        long total = docArticleRepository.count(projectId, "PUBLISHED", categoryFilter);
        List<DocView> items = docArticleRepository
                .findPage(projectId, "PUBLISHED", categoryFilter, safePageSize,
                        PaginationSupport.offset(safePageNum, safePageSize)).stream()
                .map(this::toView).toList();
        return new PageResult<>(items, total, safePageNum, safePageSize);
    }

    public DocView publicDetail(long projectId, String slug) {
        DocArticle doc = docArticleRepository.findByProjectIdAndSlug(projectId, slug)
                .filter(row -> "PUBLISHED".equals(row.status()))
                .orElseThrow(() -> ApiException.notFound("该文档不存在或尚未发布"));
        return toView(doc);
    }

    private DocArticle requireDoc(long docId) {
        return docArticleRepository.findById(docId)
                .orElseThrow(() -> ApiException.notFound("文档不存在"));
    }

    private void validate(Map<String, String> errors, String title, String category, String markdown,
            String slug, String status) {
        ContentValidation.requiredText(errors, "title", title, 120);
        ContentValidation.optionalText(errors, "category", category, 40);
        if (markdown == null || markdown.strip().isEmpty()) {
            errors.put("bodyMarkdown", "正文不能为空");
        } else if (markdown.length() > 20000) {
            errors.put("bodyMarkdown", "正文长度不能超过 20000 个字符");
        }
        if (slug == null || slug.isBlank()) {
            errors.put("slug", "不能为空");
        } else if (!SLUG_PATTERN.matcher(slug).matches()) {
            errors.put("slug", "只能由小写字母、数字和中划线组成，长度 1–63 且以字母或数字开头");
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

    private ApiException docSlugConflict() {
        return new ApiException(409, "文档标识已被占用", Map.of("slug", "文档标识已被占用"));
    }

    private DocView toView(DocArticle doc) {
        return new DocView(doc.id(), doc.title(), doc.category(), doc.bodyMarkdown(), doc.content(),
                doc.slug(), doc.status(), doc.createdAt(), doc.updatedAt());
    }
}
