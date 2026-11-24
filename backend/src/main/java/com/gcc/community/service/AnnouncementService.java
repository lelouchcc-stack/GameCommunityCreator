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
import com.gcc.community.domain.Announcement;
import com.gcc.community.dto.CreateAnnouncementRequest;
import com.gcc.community.dto.UpdateAnnouncementRequest;
import com.gcc.community.repository.AnnouncementRepository;

/** 公告的业务。 */
@Service
public class AnnouncementService {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private AnnouncementRepository announcementRepository;

    // 公告列表，置顶的在前
    public PageResult<Announcement> list(long projectId, int pageNum, int pageSize) {
        int safePageNum = PaginationSupport.requirePageNum(pageNum);
        int safePageSize = PaginationSupport.requirePageSize(pageSize);
        long total = announcementRepository.countByProjectId(projectId);
        List<Announcement> items = announcementRepository.findPage(
                projectId, safePageSize, PaginationSupport.offset(safePageNum, safePageSize));
        return new PageResult<>(items, total, safePageNum, safePageSize);
    }

    // 管理端列表，要先看看项目是不是自己的
    public PageResult<Announcement> listForAdmin(long projectId, int pageNum, int pageSize) {
        projectService.requireOwnedProject(projectId);
        return list(projectId, pageNum, pageSize);
    }

    @Transactional
    public Announcement create(long projectId, CreateAnnouncementRequest request) {
        projectService.requireOwnedProject(projectId);
        String title = request.title();
        String markdown = firstNonBlank(request.bodyMarkdown(), request.content());
        boolean pinned = Boolean.TRUE.equals(request.pinned());
        Map<String, String> errors = new LinkedHashMap<>();
        validate(errors, title, markdown);
        ContentValidation.throwIfAny("公告参数校验失败", errors);

        LocalDateTime now = LocalDateTime.now();
        long id = announcementRepository.insert(projectId, title, markdown, markdown, pinned, now);
        return new Announcement(id, title, markdown, markdown, pinned, now);
    }

    @Transactional
    public Announcement update(long announcementId, UpdateAnnouncementRequest request) {
        Long projectId = announcementRepository.findProjectIdById(announcementId)
                .orElseThrow(() -> ApiException.notFound("公告不存在"));
        projectService.requireOwnedProject(projectId);
        Announcement existing = announcementRepository.findById(announcementId)
                .orElseThrow(() -> ApiException.notFound("公告不存在"));

        String title = request.title() == null ? existing.title() : request.title();
        String markdown = firstNonBlank(request.bodyMarkdown(), request.content());
        if (markdown == null) {
            markdown = existing.bodyMarkdown();
        }
        boolean pinned = request.pinned() == null ? existing.pinned() : request.pinned();

        Map<String, String> errors = new LinkedHashMap<>();
        validate(errors, title, markdown);
        ContentValidation.throwIfAny("公告参数校验失败", errors);

        announcementRepository.update(announcementId, title, markdown, markdown, pinned);
        return new Announcement(announcementId, title, markdown, markdown, pinned,
                existing.publishedAt());
    }

    @Transactional
    public void delete(long announcementId) {
        Long projectId = announcementRepository.findProjectIdById(announcementId)
                .orElseThrow(() -> ApiException.notFound("公告不存在"));
        projectService.requireOwnedProject(projectId);
        if (announcementRepository.deleteById(announcementId) == 0) {
            throw ApiException.notFound("公告不存在");
        }
    }

    private void validate(Map<String, String> errors, String title, String markdown) {
        ContentValidation.requiredText(errors, "title", title, 120);
        if (markdown == null || markdown.strip().isEmpty()) {
            errors.put("bodyMarkdown", "公告正文不能为空");
        } else if (markdown.length() > 10000) {
            errors.put("bodyMarkdown", "公告正文长度不能超过 10000 个字符");
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
