package com.gcc.community.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.gcc.community.domain.GameInfo;

// 游戏信息表的增删改查。
@Repository
public class GameInfoRepository {

    private static final RowMapper<GameInfo> MAPPER = (rs, rowNum) -> new GameInfo(
            rs.getLong("project_id"),
            rs.getString("name"),
            rs.getString("tagline"),
            rs.getString("description"),
            rs.getString("cover_url"),
            rs.getString("platforms"),
            rs.getString("release_status"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Optional<GameInfo> findByProjectId(long projectId) {
        List<GameInfo> rows = jdbcTemplate.query(
                "SELECT project_id, name, tagline, description, cover_url, platforms, release_status "
                        + "FROM game_info WHERE project_id = ?",
                MAPPER, projectId);
        return rows.stream().findFirst();
    }

    public int insert(GameInfo info) {
        return jdbcTemplate.update(
                "INSERT INTO game_info (project_id, name, tagline, description, cover_url, platforms, release_status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                info.projectId(), info.name(), info.tagline(), info.description(),
                info.coverUrl(), info.platforms(), info.releaseStatus());
    }

    public int update(GameInfo info) {
        return jdbcTemplate.update(
                "UPDATE game_info SET name = ?, tagline = ?, description = ?, cover_url = ?, platforms = ?, "
                        + "release_status = ? WHERE project_id = ?",
                info.name(), info.tagline(), info.description(), info.coverUrl(),
                info.platforms(), info.releaseStatus(), info.projectId());
    }

    public int deleteByProjectId(long projectId) {
        return jdbcTemplate.update("DELETE FROM game_info WHERE project_id = ?", projectId);
    }
}
