package com.lelcc.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端搭建相关的 JSON 读写工具。
 * style/content/behavior 在库里以字符串存储，出参统一转成对象，前端不必再 parse。
 */
public final class UiJsonUtils {

    private UiJsonUtils() {
    }

    /** schema 字符串（JSON 数组）-> key 列表，空串得空列表 */
    public static List<String> toStringList(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        return JSONUtil.parseArray(json).toList(String.class);
    }

    /** 值字符串（JSON 对象）-> Map，空串得空 Map */
    public static Map<String, Object> toMap(String json) {
        if (StrUtil.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        JSONObject obj = JSONUtil.parseObj(json);
        return new LinkedHashMap<>(obj);
    }

    /** 值字符串（JSON 数组）-> Map 列表，空串得空列表；非对象的元素跳过 */
    public static List<Map<String, Object>> toMapList(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        JSONArray array = JSONUtil.parseArray(json);
        List<Map<String, Object>> result = new ArrayList<>(array.size());
        for (Object item : array) {
            if (item instanceof JSONObject obj) {
                result.add(new LinkedHashMap<>(obj));
            }
        }
        return result;
    }

    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return "[]";
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return "{}";
        }
        return JSONUtil.toJsonStr(value);
    }

    /** 默认值打底、覆盖值优先；用于把组件类型默认样式与节点覆盖值合并 */
    public static Map<String, Object> merge(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> merged = new LinkedHashMap<>(base);
        if (override != null) {
            merged.putAll(override);
        }
        return merged;
    }

    public static void requireJsonObject(String json, String label) {
        if (StrUtil.isBlank(json)) {
            return;
        }
        if (!JSONUtil.isTypeJSONObject(json)) {
            throw new BusinessException(400, label + "必须是合法的 JSON 对象");
        }
    }

    public static void requireJsonArray(String json, String label) {
        if (StrUtil.isBlank(json)) {
            return;
        }
        if (!JSONUtil.isTypeJSONArray(json)) {
            throw new BusinessException(400, label + "必须是合法的 JSON 数组");
        }
    }
}
