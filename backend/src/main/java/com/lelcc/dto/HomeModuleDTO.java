package com.lelcc.dto;

/** 首页模块批量保存的单行请求体，只改已有行，不支持新增模块类型。 */
public record HomeModuleDTO(
        Long id,
        String moduleType,
        String moduleName,
        Integer sort,
        Integer enabled,
        String configJson) {
}
