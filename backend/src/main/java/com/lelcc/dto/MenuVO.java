package com.lelcc.dto;

import java.util.List;

/**
 * 菜单树节点。只返回原始菜单信息，不拼前端路由的完整路径——
 * 路径拼接与组件解析交给前端，路由长什么样是前端的事。
 */
public record MenuVO(
        Long id,
        Long parentId,
        String menuName,
        String menuType,
        String path,
        String component,
        String perms,
        String icon,
        Integer sort,
        List<MenuVO> children) {
}
