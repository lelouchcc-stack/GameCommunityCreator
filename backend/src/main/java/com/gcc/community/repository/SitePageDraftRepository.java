package com.gcc.community.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.gcc.community.domain.SitePageDraft;

// 页面草稿的数据库操作，用版本号做乐观锁。
@Repository
public class SitePageDraftRepository {

    private static final RowMapper<SitePageDraft> MAPPER = (rs, rowNum) -> new SitePageDraft(
            rs.getString("page_id"),
            rs.getString("config_json"),
            rs.getInt("config_version"),
            rs.getObject("updated_at", LocalDateTime.class));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Optional<SitePageDraft> findByPageId(String pageId) {
        List<SitePageDraft> rows = jdbcTemplate.query(
                "SELECT page_id, config_json, config_version, updated_at FROM site_page_draft "
                        + "WHERE page_id = ?",
                MAPPER, pageId);
        return rows.stream().findFirst();
    }

    public int insert(SitePageDraft draft) {
        return jdbcTemplate.update(
                "INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) "
                        + "VALUES (?, ?, ?, ?)",
                draft.pageId(), draft.configJson(), draft.configVersion(), draft.updatedAt());
    }

    // 版本号没变才更新，返回 0 说明有人抢先改了
    public int updateWithVersionCheck(String pageId, String configJson, int newVersion,
            LocalDateTime updatedAt, int expectedVersion) {
        return jdbcTemplate.update(
                "UPDATE site_page_draft SET config_json = ?, config_version = ?, updated_at = ? "
                        + "WHERE page_id = ? AND config_version = ?",
                configJson, newVersion, updatedAt, pageId, expectedVersion);
    }

    // 直接覆盖，不比较版本号
    public int update(String pageId, String configJson, int newVersion, LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                "UPDATE site_page_draft SET config_json = ?, config_version = ?, updated_at = ? "
                        + "WHERE page_id = ?",
                configJson, newVersion, updatedAt, pageId);
    }

    public int deleteByPageId(String pageId) {
        return jdbcTemplate.update("DELETE FROM site_page_draft WHERE page_id = ?", pageId);
    }

    public int deleteByProjectId(long projectId) {
        return jdbcTemplate.update(
                "DELETE FROM site_page_draft WHERE page_id IN (SELECT id FROM site_page WHERE project_id = ?)",
                projectId);
    }
}
