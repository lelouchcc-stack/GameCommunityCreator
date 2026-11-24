package com.gcc.community.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.gcc.community.domain.FriendLink;

// 友情链接的数据库操作。
@Repository
public class FriendLinkRepository {

    private static final RowMapper<FriendLink> MAPPER = (rs, rowNum) -> new FriendLink(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("name"),
            rs.getString("url"),
            rs.getString("description"),
            rs.getInt("sort_order"),
            rs.getBoolean("enabled"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<FriendLink> findByProjectIdOrdered(long projectId) {
        return jdbcTemplate.query(
                "SELECT id, project_id, name, url, description, sort_order, enabled FROM friend_link "
                        + "WHERE project_id = ? ORDER BY sort_order ASC, id ASC",
                MAPPER, projectId);
    }

    public List<FriendLink> findEnabledByProjectId(long projectId) {
        return jdbcTemplate.query(
                "SELECT id, project_id, name, url, description, sort_order, enabled FROM friend_link "
                        + "WHERE project_id = ? AND enabled = ? ORDER BY sort_order ASC, id ASC",
                MAPPER, projectId, true);
    }

    public Optional<FriendLink> findById(long id) {
        List<FriendLink> rows = jdbcTemplate.query(
                "SELECT id, project_id, name, url, description, sort_order, enabled FROM friend_link "
                        + "WHERE id = ?",
                MAPPER, id);
        return rows.stream().findFirst();
    }

    public long insert(long projectId, String name, String url, String description, int sortOrder,
            boolean enabled) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO friend_link (project_id, name, url, description, sort_order, enabled) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, projectId);
            ps.setString(2, name);
            ps.setString(3, url);
            ps.setString(4, description);
            ps.setInt(5, sortOrder);
            ps.setBoolean(6, enabled);
            return ps;
        }, keyHolder);
        return GeneratedKeys.singleId(keyHolder, "friend_link");
    }

    public int update(long id, String name, String url, String description, int sortOrder, boolean enabled) {
        return jdbcTemplate.update(
                "UPDATE friend_link SET name = ?, url = ?, description = ?, sort_order = ?, enabled = ? "
                        + "WHERE id = ?",
                name, url, description, sortOrder, enabled, id);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM friend_link WHERE id = ?", id);
    }

    public int deleteByProjectId(long projectId) {
        return jdbcTemplate.update("DELETE FROM friend_link WHERE project_id = ?", projectId);
    }
}
