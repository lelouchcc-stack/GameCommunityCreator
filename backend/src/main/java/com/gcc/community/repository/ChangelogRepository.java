package com.gcc.community.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
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

import com.gcc.community.domain.Changelog;

// 更新日志的数据库操作。
@Repository
public class ChangelogRepository {

    private static final String COLUMNS =
            "id, project_id, version, title, content, body_markdown, release_date, status, created_at, updated_at";

    private static final RowMapper<Changelog> MAPPER = (rs, rowNum) -> new Changelog(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("version"),
            rs.getString("title"),
            rs.getString("content"),
            AnnouncementRepository.fallbackBodyMarkdown(rs.getString("content"),
                    rs.getString("body_markdown")),
            rs.getObject("release_date", LocalDate.class),
            rs.getString("status"),
            rs.getObject("created_at", LocalDateTime.class),
            rs.getObject("updated_at", LocalDateTime.class));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Changelog> findPage(long projectId, String status, int limit, int offset) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM changelog WHERE project_id = ?");
        args.add(projectId);
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY release_date DESC, id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), MAPPER, args.toArray());
    }

    public long count(long projectId, String status) {
        if (status == null) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM changelog WHERE project_id = ?", Long.class, projectId);
            return count == null ? 0L : count;
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM changelog WHERE project_id = ? AND status = ?",
                Long.class, projectId, status);
        return count == null ? 0L : count;
    }

    public Optional<Changelog> findById(long id) {
        List<Changelog> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM changelog WHERE id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    public long insert(long projectId, String version, String title, String markdown,
            LocalDate releaseDate, String status, LocalDateTime now) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO changelog (project_id, version, title, content, body_markdown, release_date, "
                            + "status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, projectId);
            ps.setString(2, version);
            ps.setString(3, title);
            ps.setString(4, markdown);
            ps.setString(5, markdown);
            ps.setObject(6, releaseDate);
            ps.setString(7, status);
            ps.setObject(8, now);
            ps.setObject(9, now);
            return ps;
        }, keyHolder);
        return GeneratedKeys.singleId(keyHolder, "changelog");
    }

    public int update(long id, String version, String title, String markdown, LocalDate releaseDate,
            String status, LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                "UPDATE changelog SET version = ?, title = ?, content = ?, body_markdown = ?, "
                        + "release_date = ?, status = ?, updated_at = ? WHERE id = ?",
                version, title, markdown, markdown, releaseDate, status, updatedAt, id);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM changelog WHERE id = ?", id);
    }

    public int deleteByProjectId(long projectId) {
        return jdbcTemplate.update("DELETE FROM changelog WHERE project_id = ?", projectId);
    }
}
