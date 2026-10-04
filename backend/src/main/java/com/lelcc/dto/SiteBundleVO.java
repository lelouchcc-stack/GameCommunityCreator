package com.lelcc.dto;

import java.util.List;

/**
 * 用户端首屏配置：一次请求拿全主题与首页组件树，
 * 避免用户端为了渲染首页发两次往返。
 * homeModules 是过渡字段——用户端首页改用 homePage 后即删除。
 */
public record SiteBundleVO(SiteConfigVO theme, PageTreeVO homePage, List<HomeModuleVO> homeModules) {
}
