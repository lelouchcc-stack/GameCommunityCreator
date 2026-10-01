package com.lelcc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 菜单新增/编辑请求体，对应 sys_menu 的三级节点。 */
public record SysMenuDTO(
        /** 上级菜单 ID，0 或空表示根节点 */
        Long parentId,

        @NotBlank(message = "请输入菜单名称")
        @Size(max = 50, message = "菜单名称不能超过 50 个字")
        String menuName,

        /** M目录 C菜单 F按钮 */
        @NotBlank(message = "请选择菜单类型")
        String menuType,

        @Size(max = 200, message = "路由地址不能超过 200 个字符")
        String path,

        @Size(max = 255, message = "组件路径不能超过 255 个字符")
        String component,

        @Size(max = 100, message = "权限标识不能超过 100 个字符")
        String perms,

        @Size(max = 100, message = "图标不能超过 100 个字符")
        String icon,

        @Min(value = 0, message = "排序不能小于 0")
        Integer sort,

        Integer visible,

        Integer status) {
}
