package com.gcc.community.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;

import com.gcc.community.domain.CommunityProject;
import com.gcc.community.domain.ProjectPageStats;

// 社区项目的数据库操作，SQL 都是手写的。
@Repository
public class CommunityProjectRepository {

    private static final String COLUMNS =
            "id, name, slug, game_name, description, status, owner_user_id, created_at, updated_at";

    private static final RowMapper<CommunityProject> PROJECT_MAPPER = (rs, rowNum) ->
            new CommunityProject(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("slug"),
                    rs.getString("game_name"),
                    rs.getString("description"),
                    rs.getString("status"),
                    nullableLong(rs.getObject("owner_user_id")),
                    rs.getObject("created_at", LocalDateTime.class),
                    rs.getObject("updated_at", LocalDateTime.class));

    // 项目加上首页版本和页面统计，一条 SQL 查出来
    private static final String STATS_SELECT = """
            SELECT p.id, p.name, p.slug, p.game_name, p.description, p.status, p.owner_user_id,
                   p.created_at, p.updated_at,
                   h.id AS home_page_id,
                   COALESCE(h.draft_version, 1) AS home_draft_version,
                   h.published_version AS home_published_version,
                   (SELECT COUNT(*) FROM site_page sp WHERE sp.project_id = p.id) AS page_count,
                   (SELECT COUNT(*) FROM site_page sp WHERE sp.project_id = p.id
                     AND sp.status = 'PUBLISHED' AND sp.published_version IS NOT NULL) AS published_page_count
            FROM community_project p
            LEFT JOIN site_page h ON h.project_id = p.id AND h.page_type = 'home'
            """;

    @Resource
    private JdbcTemplate jdbcTemplate;

    public List<CommunityProject> findAll() {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM community_project ORDER BY updated_at DESC, id DESC",
                PROJECT_MAPPER);
    }

    public List<ProjectPageStats> findAllWithPageStats() {
        return jdbcTemplate.query(
                STATS_SELECT + " ORDER BY p.updated_at DESC, p.id DESC",
                this::mapWithStats);
    }

    // 管理端只查某个开发者的项目
    public List<ProjectPageStats> findAllWithPageStatsByOwner(long ownerUserId) {
        return jdbcTemplate.query(
                STATS_SELECT + " WHERE p.owner_user_id = ? ORDER BY p.updated_at DESC, p.id DESC",
                this::mapWithStats, ownerUserId);
    }

    public Optional<ProjectPageStats> findWithPageStatsById(long id) {
        List<ProjectPageStats> rows = jdbcTemplate.query(
                STATS_SELECT + " WHERE p.id = ?", this::mapWithStats, id);
        return rows.stream().findFirst();
    }

    public Optional<CommunityProject> findById(long id) {
        List<CommunityProject> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM community_project WHERE id = ?", PROJECT_MAPPER, id);
        return rows.stream().findFirst();
    }

    public Optional<CommunityProject> findBySlug(String slug) {
        List<CommunityProject> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM community_project WHERE slug = ?", PROJECT_MAPPER, slug);
        return rows.stream().findFirst();
    }

    public boolean existsBySlug(String slug) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM community_project WHERE slug = ?", Integer.class, slug);
        return count != null && count > 0;
    }

    // 公开端能看到的所有已发布站点
    public List<CommunityProject> findAllPublished() {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM community_project WHERE status = 'PUBLISHED' "
                        + "ORDER BY updated_at DESC, id DESC",
                PROJECT_MAPPER);
    }

    public long insert(CommunityProject project) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO community_project (name, slug, game_name, description, status, "
                            + "owner_user_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, project.name());
            ps.setString(2, project.slug());
            ps.setString(3, project.gameName());
            ps.setString(4, project.description());
            ps.setString(5, project.status());
            ps.setObject(6, project.ownerUserId());
            ps.setObject(7, project.createdAt());
            ps.setObject(8, project.updatedAt());
            return ps;
        }, keyHolder);
        return GeneratedKeys.singleId(keyHolder, "community_project");
    }

    public int update(long id, String name, String gameName, String description, LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                "UPDATE community_project SET name = ?, game_name = ?, description = ?, updated_at = ? "
                        + "WHERE id = ?",
                name, gameName, description, updatedAt, id);
    }

    public int updateStatus(long id, String status, LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                "UPDATE community_project SET status = ?, updated_at = ? WHERE id = ?",
                status, updatedAt, id);
    }

    // 把还没有归属的旧项目认领给这个开发者，返回认领了几个
    public int claimUnownedProjects(long ownerUserId) {
        return jdbcTemplate.update(
                "UPDATE community_project SET owner_user_id = ? WHERE owner_user_id IS NULL",
                ownerUserId);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM community_project WHERE id = ?", id);
    }

    private static Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private ProjectPageStats mapWithStats(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        CommunityProject project = new CommunityProject(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("game_name"),
                rs.getString("description"),
                rs.getString("status"),
                nullableLong(rs.getObject("owner_user_id")),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
        Object publishedRaw = rs.getObject("home_published_version");
        return new ProjectPageStats(
                project,
                rs.getString("home_page_id"),
                rs.getInt("home_draft_version"),
                publishedRaw == null ? null : ((Number) publishedRaw).intValue(),
                rs.getLong("page_count"),
                rs.getLong("published_page_count"));
    }
}
