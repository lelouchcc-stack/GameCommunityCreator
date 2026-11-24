package com.gcc.community.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.gcc.community.domain.SiteSettingsRecord;

// 站点设置的数据库操作。
@Repository
public class SiteSettingsRepository {

    private static final RowMapper<SiteSettingsRecord> MAPPER = (rs, rowNum) -> new SiteSettingsRecord(
            rs.getLong("project_id"),
            rs.getString("theme_mode"),
            rs.getObject("updated_at", LocalDateTime.class));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Optional<SiteSettingsRecord> findByProjectId(long projectId) {
        List<SiteSettingsRecord> rows = jdbcTemplate.query(
                "SELECT project_id, theme_mode, updated_at FROM site_settings WHERE project_id = ?",
                MAPPER, projectId);
        return rows.stream().findFirst();
    }

    public int insert(long projectId, String themeMode, LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                "INSERT INTO site_settings (project_id, theme_mode, updated_at) VALUES (?, ?, ?)",
                projectId, themeMode, updatedAt);
    }

    public int update(long projectId, String themeMode, LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                "UPDATE site_settings SET theme_mode = ?, updated_at = ? WHERE project_id = ?",
                themeMode, updatedAt, projectId);
    }

    public int deleteByProjectId(long projectId) {
        return jdbcTemplate.update("DELETE FROM site_settings WHERE project_id = ?", projectId);
    }
}
