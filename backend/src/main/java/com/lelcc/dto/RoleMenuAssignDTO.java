package com.lelcc.dto;

import java.util.List;

/** 为角色分配菜单权限。允许为空集合，表示该角色不拥有任何菜单。 */
public record RoleMenuAssignDTO(List<Long> menuIds) {
}
