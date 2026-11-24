package com.gcc.community.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.gcc.community.domain.NavigationItem;

// 站点导航的数据库操作，保存时整批替换。
@Repository
public class SiteNavigationRepository {

    private static final String COLUMNS =
            "id, project_id, location, title, target_type, target_page_id, url, sort_order, visible";

    private static final RowMapper<NavigationItem> MAPPER = (rs, rowNum) -> new NavigationItem(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("location"),
            rs.getString("title"),
            rs.getString("target_type"),
            rs.getString("target_page_id"),
            rs.getString("url"),
            rs.getInt("sort_order"),
            rs.getBoolean("visible"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 全部导航，header 排在前面
    public List<NavigationItem> findByProjectIdOrdered(long projectId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM site_navigation WHERE project_id = ? "
                        + "ORDER BY CASE location WHEN 'header' THEN 0 ELSE 1 END, sort_order ASC, id ASC",
                MAPPER, projectId);
    }

    public List<NavigationItem> findVisibleByProjectId(long projectId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM site_navigation WHERE project_id = ? AND visible = ? "
                        + "ORDER BY CASE location WHEN 'header' THEN 0 ELSE 1 END, sort_order ASC, id ASC",
                MAPPER, projectId, true);
    }

    public int insert(long projectId, String location, String title, String targetType,
            String targetPageId, String url, int sortOrder, boolean visible) {
        return jdbcTemplate.update(
                "INSERT INTO site_navigation "
                        + "(project_id, location, title, target_type, target_page_id, url, sort_order, visible) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                projectId, location, title, targetType, targetPageId, url, sortOrder, visible);
    }

    public int deleteByProjectId(long projectId) {
        return jdbcTemplate.update("DELETE FROM site_navigation WHERE project_id = ?", projectId);
    }

    // 删页面时把指向它的导航一起删掉
    public int deleteByTargetPageId(long projectId, String targetPageId) {
        return jdbcTemplate.update(
                "DELETE FROM site_navigation WHERE project_id = ? AND target_page_id = ?",
                projectId, targetPageId);
    }
}
