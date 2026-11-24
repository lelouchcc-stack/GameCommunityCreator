package com.gcc.community.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.gcc.community.domain.SitePage;

// 站点页面的数据库操作，列表按排序号排。
@Repository
public class SitePageRepository {

    private static final String COLUMNS =
            "id, project_id, name, slug, page_type, template_key, status, sort_order, "
                    + "draft_version, published_version, created_at, updated_at";

    private static final RowMapper<SitePage> MAPPER = (rs, rowNum) -> new SitePage(
            rs.getString("id"),
            rs.getLong("project_id"),
            rs.getString("name"),
            rs.getString("slug"),
            rs.getString("page_type"),
            rs.getString("template_key"),
            rs.getString("status"),
            rs.getInt("sort_order"),
            rs.getInt("draft_version"),
            rs.getObject("published_version") == null ? null : rs.getInt("published_version"),
            rs.getObject("created_at", LocalDateTime.class),
            rs.getObject("updated_at", LocalDateTime.class));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<SitePage> findByProjectIdOrdered(long projectId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM site_page WHERE project_id = ? "
                        + "ORDER BY sort_order ASC, created_at ASC, id ASC",
                MAPPER, projectId);
    }

    public Optional<SitePage> findById(String pageId) {
        List<SitePage> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM site_page WHERE id = ?", MAPPER, pageId);
        return rows.stream().findFirst();
    }

    public Optional<SitePage> findByProjectIdAndSlug(long projectId, String slug) {
        List<SitePage> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM site_page WHERE project_id = ? AND slug = ?",
                MAPPER, projectId, slug);
        return rows.stream().findFirst();
    }

    public Optional<SitePage> findFirstByProjectIdAndPageType(long projectId, String pageType) {
        List<SitePage> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM site_page WHERE project_id = ? AND page_type = ? "
                        + "ORDER BY sort_order ASC, id ASC LIMIT 1",
                MAPPER, projectId, pageType);
        return rows.stream().findFirst();
    }

    public Optional<SitePage> findHomePage(long projectId) {
        return findFirstByProjectIdAndPageType(projectId, "home");
    }

    public boolean existsByProjectIdAndPageType(long projectId, String pageType) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM site_page WHERE project_id = ? AND page_type = ?",
                Integer.class, projectId, pageType);
        return count != null && count > 0;
    }

    public long countByProjectId(long projectId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM site_page WHERE project_id = ?", Long.class, projectId);
        return count == null ? 0L : count;
    }

    public int maxSortOrder(long projectId) {
        Integer max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_order), -1) FROM site_page WHERE project_id = ?",
                Integer.class, projectId);
        return max == null ? -1 : max;
    }

    public int insert(SitePage page) {
        return jdbcTemplate.update(
                "INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, "
                        + "sort_order, draft_version, published_version, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                page.id(), page.projectId(), page.name(), page.slug(), page.pageType(),
                page.templateKey(), page.status(), page.sortOrder(), page.draftVersion(),
                page.publishedVersion(), page.createdAt(), page.updatedAt());
    }

    // 更新页面的基本信息
    public int updateMeta(String pageId, String name, String slug, String status, String templateKey,
            LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                "UPDATE site_page SET name = ?, slug = ?, status = ?, template_key = ?, updated_at = ? "
                        + "WHERE id = ?",
                name, slug, status, templateKey, updatedAt, pageId);
    }

    public int updateDraftVersion(String pageId, int draftVersion, LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                "UPDATE site_page SET draft_version = ?, updated_at = ? WHERE id = ?",
                draftVersion, updatedAt, pageId);
    }

    // 发布页面，记下已发布的版本号
    public int updatePublishState(String pageId, String status, int publishedVersion,
            LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                "UPDATE site_page SET status = ?, published_version = ?, updated_at = ? WHERE id = ?",
                status, publishedVersion, updatedAt, pageId);
    }

    public int updateStatus(String pageId, String status, LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                "UPDATE site_page SET status = ?, updated_at = ? WHERE id = ?",
                status, updatedAt, pageId);
    }

    public int deleteById(String pageId) {
        return jdbcTemplate.update("DELETE FROM site_page WHERE id = ?", pageId);
    }

    public int deleteByProjectId(long projectId) {
        return jdbcTemplate.update("DELETE FROM site_page WHERE project_id = ?", projectId);
    }
}
