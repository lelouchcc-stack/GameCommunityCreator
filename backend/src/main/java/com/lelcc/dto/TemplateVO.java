package com.lelcc.dto;

import java.util.List;
import java.util.Map;

/**
 * 一个可用的社区模板（读自 classpath:templates/&lt;code&gt;/manifest.json）。
 *
 * <p>inputs 是模板自己声明的输入项，管理端的「创建社区」表单按它动态渲染——
 * 后端与前端都不写死字段清单，加一个模板（或给模板加一个输入）不用改 Java 与 Vue。</p>
 */
public record TemplateVO(
        String code,
        String name,
        String nameZh,
        Integer version,
        String description,
        List<InputVO> inputs,
        List<String> pageKeys,
        /** 模板依赖但不覆盖的既有页面：pageKey -> 说明 */
        Map<String, String> relatedPages,
        /** 模板认识但已废弃、可由管理员自行删除的页面：pageKey -> 说明 */
        Map<String, String> customPages) {

    /** 模板输入项：type 为 text / textarea / color / image / boolean */
    public record InputVO(
            String key,
            String label,
            String type,
            Boolean required,
            Integer maxLength,
            String placeholder,
            String help,
            String defaultValue) {
    }
}
