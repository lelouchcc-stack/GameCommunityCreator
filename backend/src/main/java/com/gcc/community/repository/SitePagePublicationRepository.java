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

import com.gcc.community.domain.PagePublicationSummary;
import com.gcc.community.domain.SitePagePublication;

// 页面发布快照的数据库操作，公开端只读这张表。
@Repository
public class SitePagePublicationRepository {

    private static final String COLUMNS =
            "id, page_id, project_id, config_version, config_json, published_at";

    private static final RowMapper<SitePagePublication> MAPPER =
            (rs, rowNum) -> new SitePagePublication(
                    rs.getLong("id"),
                    rs.getString("page_id"),
                    rs.getLong("project_id"),
                    rs.getInt("config_version"),
                    rs.getString("config_json"),
                    rs.getObject("published_at", LocalDateTime.class));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public long insert(String pageId, long projectId, int configVersion, String configJson,
            LocalDateTime publishedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO site_page_publication "
                            + "(page_id, project_id, config_version, config_json, published_at) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, pageId);
            ps.setLong(2, projectId);
            ps.setInt(3, configVersion);
            ps.setString(4, configJson);
            ps.setObject(5, publishedAt);
            return ps;
        }, keyHolder);
        return GeneratedKeys.singleId(keyHolder, "site_page_publication");
    }

    public boolean existsByPageIdAndConfigVersion(String pageId, int configVersion) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM site_page_publication WHERE page_id = ? AND config_version = ?",
                Integer.class, pageId, configVersion);
        return count != null && count > 0;
    }

    // 取这个页面最新的一次发布
    public Optional<SitePagePublication> findLatestByPageId(String pageId) {
        List<SitePagePublication> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM site_page_publication WHERE page_id = ? "
                        + "ORDER BY config_version DESC, id DESC LIMIT 1",
                MAPPER, pageId);
        return rows.stream().findFirst();
    }

    // 发布历史列表，不带具体配置内容
    public List<PagePublicationSummary> findSummariesByPageId(String pageId) {
        return jdbcTemplate.query(
                "SELECT id, config_version, published_at FROM site_page_publication WHERE page_id = ? "
                        + "ORDER BY published_at DESC, id DESC",
                (rs, rowNum) -> new PagePublicationSummary(
                        rs.getLong("id"),
                        rs.getInt("config_version"),
                        rs.getObject("published_at", LocalDateTime.class)),
                pageId);
    }

    // 站点所有发布记录，公开端自己取每页最新的
    public List<SitePagePublication> findAllByProjectId(long projectId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM site_page_publication WHERE project_id = ? "
                        + "ORDER BY page_id ASC, config_version DESC, id DESC",
                MAPPER, projectId);
    }

    public int deleteByPageId(String pageId) {
        return jdbcTemplate.update("DELETE FROM site_page_publication WHERE page_id = ?", pageId);
    }

    public int deleteByProjectId(long projectId) {
        return jdbcTemplate.update("DELETE FROM site_page_publication WHERE project_id = ?", projectId);
    }
}
