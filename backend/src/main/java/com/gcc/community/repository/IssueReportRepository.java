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

import com.gcc.community.domain.IssueReport;

// 问题反馈的数据库操作。
@Repository
public class IssueReportRepository {

    private static final String COLUMNS =
            "id, project_id, title, category, description, game_version, steps, status, resolution, "
                    + "is_public, created_at, updated_at";

    private static final RowMapper<IssueReport> MAPPER = (rs, rowNum) -> new IssueReport(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("title"),
            rs.getString("category"),
            rs.getString("description"),
            rs.getString("game_version"),
            rs.getString("steps"),
            rs.getString("status"),
            rs.getString("resolution"),
            rs.getBoolean("is_public"),
            rs.getObject("created_at", LocalDateTime.class),
            rs.getObject("updated_at", LocalDateTime.class));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<IssueReport> findPage(long projectId, String status, int limit, int offset) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM issue_report WHERE project_id = ?");
        args.add(projectId);
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), MAPPER, args.toArray());
    }

    public long count(long projectId, String status) {
        if (status == null) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM issue_report WHERE project_id = ?", Long.class, projectId);
            return count == null ? 0L : count;
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM issue_report WHERE project_id = ? AND status = ?",
                Long.class, projectId, status);
        return count == null ? 0L : count;
    }

    // 公开进展：只要已解决并且同意公开的，按更新时间倒序
    public List<IssueReport> findPublicProgress(long projectId, int limit) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM issue_report "
                        + "WHERE project_id = ? AND status = 'RESOLVED' AND is_public = ? "
                        + "ORDER BY updated_at DESC, id DESC LIMIT ?",
                MAPPER, projectId, true, limit);
    }

    public Optional<IssueReport> findById(long id) {
        List<IssueReport> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM issue_report WHERE id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    public long insert(long projectId, String title, String category, String description,
            String gameVersion, String steps, String status, LocalDateTime now) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO issue_report (project_id, title, category, description, game_version, steps, "
                            + "status, resolution, is_public, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, '', 0, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, projectId);
            ps.setString(2, title);
            ps.setString(3, category);
            ps.setString(4, description);
            ps.setString(5, gameVersion);
            ps.setString(6, steps);
            ps.setString(7, status);
            ps.setObject(8, now);
            ps.setObject(9, now);
            return ps;
        }, keyHolder);
        return GeneratedKeys.singleId(keyHolder, "issue_report");
    }

    public int update(long id, String status, String resolution, boolean isPublic, LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                "UPDATE issue_report SET status = ?, resolution = ?, is_public = ?, updated_at = ? "
                        + "WHERE id = ?",
                status, resolution, isPublic ? 1 : 0, updatedAt, id);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM issue_report WHERE id = ?", id);
    }

    public int deleteByProjectId(long projectId) {
        return jdbcTemplate.update("DELETE FROM issue_report WHERE project_id = ?", projectId);
    }
}
