package com.gcc.community.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gcc.community.common.ApiResponse;
import com.gcc.community.common.ApiException;

/** 健康检查接口，看看数据库还能不能连上。 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception ex) {
            throw new ApiException(500, "数据库连接不可用");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("database", "UP");
        data.put("time", LocalDateTime.now().toString());
        return ApiResponse.ok(data);
    }
}
