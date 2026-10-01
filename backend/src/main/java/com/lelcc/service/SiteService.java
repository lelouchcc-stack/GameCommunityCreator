package com.lelcc.service;

import com.lelcc.dto.HomeModuleDTO;
import com.lelcc.dto.HomeModuleVO;
import com.lelcc.dto.SiteBundleVO;
import com.lelcc.dto.SiteConfigDTO;
import com.lelcc.dto.SiteConfigVO;

import java.util.List;

public interface SiteService {

    /** 读取站点信息/主题，缺失的配置键用注册表默认值补齐 */
    SiteConfigVO getConfig();

    /** 保存站点信息/主题，返回保存后的最新配置供管理端即时预览 */
    SiteConfigVO updateConfig(SiteConfigDTO dto);

    List<HomeModuleVO> listModules();

    /** 整表批量保存（拖拽排序一次提交），返回保存后的列表 */
    List<HomeModuleVO> saveModules(List<HomeModuleDTO> modules);

    /** 用户端首屏配置：主题 + 仅启用的模块（按 sort 升序） */
    SiteBundleVO userBundle();
}
