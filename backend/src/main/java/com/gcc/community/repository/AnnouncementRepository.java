package com.gcc.community.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.gcc.community.domain.Announcement;

// 公告的数据库操作，置顶的排前面。
@Repository
public class AnnouncementRepository {

    private static final String COLUMNS = "id, project_id, title, content, body_markdown, pinned, published_at";

    private static final RowMapper<Announcement> MAPPER = (rs, rowNum) -> new Announcement(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("content"),
            fallbackBodyMarkdown(rs.getString("content"), rs.getString("body_markdown")),
            rs.getBoolean("pinned"),
            rs.getObject("published_at", LocalDateTime.class));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Announcement> findPage(long projectId, int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM announcement WHERE project_id = ? "
                        + "ORDER BY pinned DESC, published_at DESC, id DESC LIMIT ? OFFSET ?",
                MAPPER, projectId, limit, offset);
    }

    public long countByProjectId(long projectId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM announcement WHERE project_id = ?", Long.class, projectId);
        return count == null ? 0L : count;
    }

    public long insert(long projectId, String title, String content, String bodyMarkdown, boolean pinned,
            LocalDateTime publishedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO announcement (project_id, title, content, body_markdown, pinned, published_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, projectId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.setString(4, bodyMarkdown);
            ps.setInt(5, pinned ? 1 : 0);
            ps.setObject(6, publishedAt);
            return ps;
        }, keyHolder);
        return GeneratedKeys.singleId(keyHolder, "announcement");
    }

    public Optional<Announcement> findById(long id) {
        List<Announcement> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM announcement WHERE id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    // 根据公告 id 查它属于哪个项目
    public Optional<Long> findProjectIdById(long id) {
        List<Long> rows = jdbcTemplate.query(
                "SELECT project_id FROM announcement WHERE id = ?",
                (rs, rowNum) -> rs.getLong("project_id"), id);
        return rows.stream().findFirst();
    }

    public int update(long id, String title, String content, String bodyMarkdown, boolean pinned) {
        return jdbcTemplate.update(
                "UPDATE announcement SET title = ?, content = ?, body_markdown = ?, pinned = ? WHERE id = ?",
                title, content, bodyMarkdown, pinned ? 1 : 0, id);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM announcement WHERE id = ?", id);
    }

    public int deleteByProjectId(long projectId) {
        return jdbcTemplate.update("DELETE FROM announcement WHERE project_id = ?", projectId);
    }

    static String fallbackBodyMarkdown(String content, String bodyMarkdown) {
        return bodyMarkdown == null || bodyMarkdown.isEmpty() ? content : bodyMarkdown;
    }
}
