package com.lelcc.dto;

import java.util.List;
import java.util.Map;

/**
 * 用户端的一个页面：页面信息 + 可见节点树 + 该页自己的逻辑图。
 *
 * <p>逻辑图必须按页下发：flowKey（init/submit...）只在页内唯一，
 * 多页平铺成一张表会键冲撞，前端也就无从判断该跑哪张图。</p>
 */
public record PageBundleVO(String pageKey,
                           String pageName,
                           String routePath,
                           Integer isHome,
                           List<NodeVO> nodes,
                           Map<String, Map<String, Object>> flows) {
}
