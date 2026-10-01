package com.lelcc.dto;

import java.util.List;

/**
 * 用户端首屏配置：一次请求拿全主题与首页模块，
 * 避免用户端为了渲染首页发两次往返。
 */
public record SiteBundleVO(SiteConfigVO theme, List<HomeModuleVO> homeModules) {
}
