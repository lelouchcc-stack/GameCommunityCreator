package com.lelcc.common;

import java.util.List;
import java.util.Map;

/**
 * 节点上的 style/content/behavior 值校验：键必须在该组件类型的 schema 内，
 * 值按属性库类型校验。白名单优先——未知键直接拒绝，不做静默丢弃。
 */
public final class UiValueValidator {

    /** 业务组件开放多区域样式后，单个节点的样式键可能较多，上限放宽到 160 */
    private static final int MAX_KEYS = 160;

    private UiValueValidator() {
    }

    /**
     * @param schema 组件类型开放的属性 key 列表
     * @param values 节点上填的值
     * @param label  出错提示前缀，如"按钮 / 样式"
     * @return 通过返回 null，否则返回中文提示
     */
    public static String validate(List<String> schema, Map<String, Object> values, String label) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() > MAX_KEYS) {
            return label + "属性项过多";
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            if (!schema.contains(key)) {
                return label + "包含未开放的属性：" + key;
            }
            String error = ComponentPropertyRegistry.validate(key, entry.getValue());
            if (error != null) {
                return label + "：" + error;
            }
        }
        return null;
    }
}
