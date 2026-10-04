package com.lelcc.dto;

/**
 * 用户端首屏配置：一次请求拿全主题与首页组件树，
 * 避免用户端为了渲染首页发两次往返。
 */
public record SiteBundleVO(SiteConfigVO theme, PageTreeVO homePage) {
}
