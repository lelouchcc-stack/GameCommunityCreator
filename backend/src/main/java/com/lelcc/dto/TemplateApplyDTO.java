package com.lelcc.dto;

import java.util.Map;

/**
 * 应用模板的请求体。
 *
 * <p>values 的键就是模板 manifest 里声明的 input key。用 Map 而不是一串固定字段，
 * 是为了让「加模板 = 加数据目录」成立：新增模板只要有新的输入项，后端不用跟着改 DTO。</p>
 */
public record TemplateApplyDTO(String code, Map<String, Object> values) {
}
