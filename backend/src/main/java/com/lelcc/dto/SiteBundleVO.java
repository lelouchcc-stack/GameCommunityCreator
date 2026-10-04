package com.lelcc.dto;

import java.util.Map;

/**
 * 用户端首屏配置：一次请求拿全主题、首页组件树、首页的逻辑图、以及图里引用到的预设定义，
 * 避免用户端为了渲染首页发多次往返。
 *
 * <p>flows 的键是 flowKey（init/submit...），值是图本体；
 * presets 的键是 "presetCode@version"（引用方锁定版本，同一预设的不同版本各带一份），
 * 值是该预设的子图，供前端执行器把预设节点展开。</p>
 */
public record SiteBundleVO(SiteConfigVO theme,
                           PageTreeVO homePage,
                           Map<String, Map<String, Object>> flows,
                           Map<String, Map<String, Object>> presets) {
}
