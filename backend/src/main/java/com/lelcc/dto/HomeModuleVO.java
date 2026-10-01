package com.lelcc.dto;

/** 首页模块，管理端配置页与用户端首页渲染共用。 */
public record HomeModuleVO(
        Long id,
        String moduleType,
        String moduleName,
        Integer sort,
        Integer enabled,
        String configJson) {
}
