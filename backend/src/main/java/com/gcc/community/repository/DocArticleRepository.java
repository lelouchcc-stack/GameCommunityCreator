package com.gcc.community.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.gcc.community.domain.DocArticle;

// 文档文章的数据库操作，slug 在站内不能重复。
@Repository
public class DocArticleRepository {

    private static final String COLUMNS =
            "id, project_id, title, category, content, body_markdown, slug, status, created_at, updated_at";

    private static final RowMapper<DocArticle> MAPPER = (rs, rowNum) -> new DocArticle(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("title"),
            rs.getString("category"),
            rs.getString("content"),
            AnnouncementRepository.fallbackBodyMarkdown(rs.getString("content"),
                    rs.getString("body_markdown")),
            rs.getString("slug"),
            rs.getString("status"),
            rs.getObject("created_at", LocalDateTime.class),
            rs.getObject("updated_at", LocalDateTime.class));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<DocArticle> findPage(long projectId, String status, String category, int limit, int offset) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM doc_article WHERE project_id = ?");
        args.add(projectId);
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            args.add(category);
        }
        sql.append(" ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), MAPPER, args.toArray());
    }

    public long count(long projectId, String status, String category) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM doc_article WHERE project_id = ?");
        args.add(projectId);
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            args.add(category);
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0L : count;
    }

    public Optional<DocArticle> findById(long id) {
        List<DocArticle> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM doc_article WHERE id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    public Optional<DocArticle> findByProjectIdAndSlug(long projectId, String slug) {
        List<DocArticle> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM doc_article WHERE project_id = ? AND slug = ?",
                MAPPER, projectId, slug);
        return rows.stream().findFirst();
    }

    public boolean existsSlug(long projectId, String slug, Long excludeId) {
        String sql = "SELECT COUNT(*) FROM doc_article WHERE project_id = ? AND slug = ?"
                + (excludeId == null ? "" : " AND id <> ?");
        Integer count = excludeId == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, projectId, slug)
                : jdbcTemplate.queryForObject(sql, Integer.class, projectId, slug, excludeId);
        return count != null && count > 0;
    }

    public long insert(long projectId, String title, String category, String markdown, String slug,
            String status, LocalDateTime now) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO doc_article (project_id, title, category, content, body_markdown, slug, "
                            + "status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, projectId);
            ps.setString(2, title);
            ps.setString(3, category);
            ps.setString(4, markdown);
            ps.setString(5, markdown);
            ps.setString(6, slug);
            ps.setString(7, status);
            ps.setObject(8, now);
            ps.setObject(9, now);
            return ps;
        }, keyHolder);
        return GeneratedKeys.singleId(keyHolder, "doc_article");
    }

    public int update(long id, String title, String category, String markdown, String slug, String status,
            LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                "UPDATE doc_article SET title = ?, category = ?, content = ?, body_markdown = ?, slug = ?, "
                        + "status = ?, updated_at = ? WHERE id = ?",
                title, category, markdown, markdown, slug, status, updatedAt, id);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM doc_article WHERE id = ?", id);
    }

    public int deleteByProjectId(long projectId) {
        return jdbcTemplate.update("DELETE FROM doc_article WHERE project_id = ?", projectId);
    }
}
