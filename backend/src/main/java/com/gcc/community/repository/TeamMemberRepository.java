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

import com.gcc.community.domain.TeamMember;

// 制作组成员的数据库操作。
@Repository
public class TeamMemberRepository {

    private static final RowMapper<TeamMember> MAPPER = (rs, rowNum) -> new TeamMember(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("name"),
            rs.getString("avatar_url"),
            rs.getString("role"),
            rs.getString("bio"),
            rs.getInt("sort_order"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<TeamMember> findByProjectIdOrdered(long projectId) {
        return jdbcTemplate.query(
                "SELECT id, project_id, name, avatar_url, role, bio, sort_order FROM team_member "
                        + "WHERE project_id = ? ORDER BY sort_order ASC, id ASC",
                MAPPER, projectId);
    }

    public Optional<TeamMember> findById(long id) {
        List<TeamMember> rows = jdbcTemplate.query(
                "SELECT id, project_id, name, avatar_url, role, bio, sort_order FROM team_member "
                        + "WHERE id = ?",
                MAPPER, id);
        return rows.stream().findFirst();
    }

    public long insert(long projectId, String name, String avatarUrl, String role, String bio,
            int sortOrder) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO team_member (project_id, name, avatar_url, role, bio, sort_order) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, projectId);
            ps.setString(2, name);
            ps.setString(3, avatarUrl);
            ps.setString(4, role);
            ps.setString(5, bio);
            ps.setInt(6, sortOrder);
            return ps;
        }, keyHolder);
        return GeneratedKeys.singleId(keyHolder, "team_member");
    }

    public int update(long id, String name, String avatarUrl, String role, String bio, int sortOrder) {
        return jdbcTemplate.update(
                "UPDATE team_member SET name = ?, avatar_url = ?, role = ?, bio = ?, sort_order = ? "
                        + "WHERE id = ?",
                name, avatarUrl, role, bio, sortOrder, id);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM team_member WHERE id = ?", id);
    }

    public int deleteByProjectId(long projectId) {
        return jdbcTemplate.update("DELETE FROM team_member WHERE project_id = ?", projectId);
    }
}
