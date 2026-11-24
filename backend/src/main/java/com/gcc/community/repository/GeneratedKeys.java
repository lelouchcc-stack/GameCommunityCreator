package com.gcc.community.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.support.KeyHolder;

// 从 KeyHolder 里取自增主键，MySQL 和 H2 都能用。
final class GeneratedKeys {

    private GeneratedKeys() {
    }

    static long singleId(KeyHolder keyHolder, String table) {
        List<Map<String, Object>> keyList = keyHolder.getKeyList();
        if (keyList.isEmpty()) {
            throw new IllegalStateException("向 " + table + " 插入数据后未返回自增主键");
        }
        Map<String, Object> row = keyList.get(0);
        Object value = row.size() == 1 ? row.values().iterator().next() : row.get("id");
        if (!(value instanceof Number number)) {
            value = row.get("GENERATED_KEY");
            if (!(value instanceof Number fallback)) {
                throw new IllegalStateException("无法解析 " + table + " 的自增主键: " + row);
            }
            return fallback.longValue();
        }
        return number.longValue();
    }
}
