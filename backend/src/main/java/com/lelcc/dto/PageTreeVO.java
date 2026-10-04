package com.lelcc.dto;

import lombok.Data;

import java.util.List;

/** 用户端页面树：页面信息 + 组件节点树。 */
@Data
public class PageTreeVO {

    private String pageKey;

    private String pageName;

    private String routePath;

    private List<NodeVO> nodes;
}
