package com.lelcc.dto;

import java.util.List;
import java.util.Map;

/**
 * 用户端首屏配置（v2，多页）：一次请求拿全主题、所有启用页面（各带节点树与逻辑图）、
 * 以及这些页的图引用到的预设定义，避免用户端为渲染一页发多次往返。
 *
 * <p>pages 里每个页面自带 flows：flowKey 只在页内唯一，平铺成一张表在多页时会键冲撞。
 * 前端按 routePath 找到页面后，用该页的 flows 建图执行器。</p>
 *
 * <p>presets 的键是 "presetCode@version"（引用方锁定版本，同一预设的不同版本各带一份），
 * 值是该预设的子图，供前端执行器把预设节点展开；只下发被引用到的，不塞整个预设库。</p>
 *
 * <p>storage 给出存储 provider 与对外访问基址：页面里存的是 objectKey，
 * 用户端据此把 key 现拼成图片地址（换存储/换 CDN 不用迁移数据）。</p>
 */
public record SiteBundleVO(SiteConfigVO theme,
                           StorageConfigVO storage,
                           Map<String, Map<String, Object>> presets,
                           List<PageBundleVO> pages) {
}
