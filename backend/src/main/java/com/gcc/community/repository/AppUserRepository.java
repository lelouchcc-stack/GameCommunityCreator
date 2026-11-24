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

import com.gcc.community.domain.AppUser;

// 开发者账号的数据库操作。
@Repository
public class AppUserRepository {

    private static final String COLUMNS = "id, username, password_hash, display_name, created_at";

    private static final RowMapper<AppUser> MAPPER = (rs, rowNum) -> new AppUser(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("display_name"),
            rs.getObject("created_at", LocalDateTime.class));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Optional<AppUser> findById(long id) {
        List<AppUser> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM app_user WHERE id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    public Optional<AppUser> findByUsername(String username) {
        List<AppUser> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM app_user WHERE username = ?", MAPPER, username);
        return rows.stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE username = ?", Integer.class, username);
        return count != null && count > 0;
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        return count == null ? 0L : count;
    }

    public long insert(String username, String passwordHash, String displayName, LocalDateTime createdAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO app_user (username, password_hash, display_name, created_at) "
                            + "VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, displayName);
            ps.setObject(4, createdAt);
            return ps;
        }, keyHolder);
        return GeneratedKeys.singleId(keyHolder, "app_user");
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM app_user WHERE id = ?", id);
    }
}
