package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.lelcc.common.BusinessException;
import com.lelcc.common.SiteConfigRegistry;
import com.lelcc.common.UiJsonUtils;
import com.lelcc.dto.HomeModuleDTO;
import com.lelcc.dto.HomeModuleVO;
import com.lelcc.dto.PageBundleVO;
import com.lelcc.dto.PageTreeVO;
import com.lelcc.dto.SiteBundleVO;
import com.lelcc.dto.SiteConfigDTO;
import com.lelcc.dto.SiteConfigVO;
import com.lelcc.dto.StorageConfigVO;
import com.lelcc.entity.HomeModule;
import com.lelcc.entity.SiteConfig;
import com.lelcc.entity.UiFlow;
import com.lelcc.entity.UiPage;
import com.lelcc.entity.UiPreset;
import com.lelcc.mapper.HomeModuleMapper;
import com.lelcc.mapper.SiteConfigMapper;
import com.lelcc.mapper.UiFlowMapper;
import com.lelcc.mapper.UiPageMapper;
import com.lelcc.mapper.UiPresetMapper;
import com.lelcc.service.FileStorage;
import com.lelcc.service.SiteService;
import com.lelcc.service.UiPageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SiteServiceImpl implements SiteService {

    private static final int ENABLED = 1;
    /** 图里引用预设用的伪 typeCode（不是原子库里的原子） */
    private static final String PRESET_TYPE = "preset";

    @Resource
    private SiteConfigMapper siteConfigMapper;

    @Resource
    private HomeModuleMapper homeModuleMapper;

    @Resource
    private UiPageService pageService;

    @Resource
    private UiPageMapper pageMapper;

    @Resource
    private UiFlowMapper flowMapper;

    @Resource
    private UiPresetMapper presetMapper;

    /** 下发存储基址，用户端据此把库里的 objectKey 现拼成可显示的图片地址 */
    @Resource
    private FileStorage fileStorage;

    @Override
    public SiteConfigVO getConfig() {
        Map<String, String> values = new HashMap<>();
        for (SiteConfig row : siteConfigMapper.selectList(null)) {
            values.put(row.getConfigKey(), row.getConfigValue());
        }
        return toVO(values);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SiteConfigVO updateConfig(SiteConfigDTO dto) {
        Map<String, String> incoming = new LinkedHashMap<>();
        incoming.put(SiteConfigRegistry.SITE_NAME, dto.siteName());
        incoming.put(SiteConfigRegistry.SITE_INTRO, dto.siteIntro());
        incoming.put(SiteConfigRegistry.LOGO, dto.logo());
        incoming.put(SiteConfigRegistry.COPYRIGHT, dto.copyright());
        incoming.put(SiteConfigRegistry.PRIMARY_COLOR, dto.primaryColor());
        incoming.put(SiteConfigRegistry.DARK_MODE, Boolean.TRUE.equals(dto.darkMode()) ? "true" : "false");

        // 先整体校验再落库：任一项不合法就整体拒绝，不写入半套配置
        Map<String, String> normalized = new LinkedHashMap<>();
        for (SiteConfigRegistry.Spec spec : SiteConfigRegistry.specs()) {
            String value = StrUtil.nullToEmpty(incoming.get(spec.key()));
            String error = spec.validate(value);
            if (error != null) {
                throw new BusinessException(400, error);
            }
            normalized.put(spec.key(), value);
        }
        normalized.forEach(this::upsert);

        return getConfig();
    }

    @Override
    public List<HomeModuleVO> listModules() {
        return homeModuleMapper.selectList(Wrappers.<HomeModule>lambdaQuery()
                        .orderByAsc(HomeModule::getSort)
                        .orderByAsc(HomeModule::getId))
                .stream()
                .map(this::toModuleVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<HomeModuleVO> saveModules(List<HomeModuleDTO> modules) {
        if (modules == null || modules.isEmpty()) {
            throw new BusinessException(400, "首页模块不能为空");
        }
        for (HomeModuleDTO dto : modules) {
            validateModule(dto);
        }
        for (HomeModuleDTO dto : modules) {
            HomeModule update = new HomeModule();
            update.setId(dto.id());
            update.setModuleName(dto.moduleName());
            update.setSort(dto.sort());
            update.setEnabled(dto.enabled());
            update.setConfigJson(dto.configJson());
            homeModuleMapper.updateById(update);
        }
        return listModules();
    }

    @Override
    public SiteBundleVO userBundle() {
        // 只下发启用页面；首页排最前，前端可据此取默认落点
        List<UiPage> pages = pageMapper.selectList(Wrappers.<UiPage>lambdaQuery()
                .eq(UiPage::getStatus, ENABLED)
                .orderByDesc(UiPage::getIsHome)
                .orderByAsc(UiPage::getId));

        List<PageBundleVO> bundles = new ArrayList<>();
        List<Map<String, Object>> allGraphs = new ArrayList<>();
        for (UiPage page : pages) {
            PageTreeVO tree = pageService.userTree(page.getPageKey());
            Map<String, Map<String, Object>> flows = pageFlows(page.getId());
            allGraphs.addAll(flows.values());
            bundles.add(new PageBundleVO(tree.getPageKey(), tree.getPageName(), tree.getRoutePath(),
                    page.getIsHome(), tree.getNodes(), flows));
        }
        return new SiteBundleVO(getConfig(),
                new StorageConfigVO(fileStorage.provider(), fileStorage.publicBaseUrl()),
                referencedPresets(allGraphs),
                bundles);
    }

    /** 用户端只拿启用中的图；键即 flowKey，前端按触发器（on.click/on.submit）对应的 flowKey 找图 */
    private Map<String, Map<String, Object>> pageFlows(Long pageId) {
        Map<String, Map<String, Object>> flows = new LinkedHashMap<>();
        for (UiFlow flow : flowMapper.selectList(Wrappers.<UiFlow>lambdaQuery()
                .eq(UiFlow::getPageId, pageId)
                .eq(UiFlow::getStatus, ENABLED)
                .orderByAsc(UiFlow::getFlowKey))) {
            flows.put(flow.getFlowKey(), UiJsonUtils.toMap(flow.getGraphJson()));
        }
        return flows;
    }

    /**
     * 只下发图里引用到的预设（含"预设引用预设"的传递闭包），不把整个预设库塞给每个页面。
     * 预设被停用或删除时跳过而不报错：前端遇到未知预设自己告警，不因此让整站配置拉不出来。
     */
    private Map<String, Map<String, Object>> referencedPresets(Collection<Map<String, Object>> graphs) {
        Map<String, Map<String, Object>> presets = new LinkedHashMap<>();
        Deque<Map<String, Object>> queue = new ArrayDeque<>(graphs);
        while (!queue.isEmpty()) {
            for (String key : collectPresetRefs(queue.poll())) {
                if (presets.containsKey(key)) {
                    continue;
                }
                int at = key.lastIndexOf('@');
                UiPreset preset = presetMapper.selectOne(Wrappers.<UiPreset>lambdaQuery()
                        .eq(UiPreset::getPresetCode, key.substring(0, at))
                        .eq(UiPreset::getVersion, key.substring(at + 1))
                        .eq(UiPreset::getStatus, ENABLED));
                if (preset == null) {
                    continue;
                }
                Map<String, Object> graph = UiJsonUtils.toMap(preset.getGraphJson());
                presets.put(key, graph);
                queue.add(graph);
            }
        }
        return presets;
    }

    private List<String> collectPresetRefs(Map<String, Object> graph) {
        List<String> keys = new ArrayList<>();
        Object nodesRaw = graph == null ? null : graph.get("nodes");
        if (!(nodesRaw instanceof List<?> nodes)) {
            return keys;
        }
        for (Object item : nodes) {
            if (!(item instanceof Map<?, ?> node) || !PRESET_TYPE.equals(String.valueOf(node.get("typeCode")))) {
                continue;
            }
            Object code = node.get("presetCode");
            if (code != null) {
                keys.add(code + "@" + node.get("presetVersion"));
            }
        }
        return keys;
    }

    private void upsert(String key, String value) {
        SiteConfig exists = siteConfigMapper.selectOne(Wrappers.<SiteConfig>lambdaQuery()
                .eq(SiteConfig::getConfigKey, key));
        if (exists == null) {
            SiteConfig row = new SiteConfig();
            row.setConfigKey(key);
            row.setConfigValue(value);
            row.setRemark(SiteConfigRegistry.get(key).label());
            siteConfigMapper.insert(row);
            return;
        }
        SiteConfig row = new SiteConfig();
        row.setId(exists.getId());
        row.setConfigValue(value);
        siteConfigMapper.updateById(row);
    }

    private void validateModule(HomeModuleDTO dto) {
        if (dto.id() == null) {
            throw new BusinessException(400, "模块 id 不能为空");
        }
        if (StrUtil.isBlank(dto.moduleType())) {
            throw new BusinessException(400, "模块类型不能为空");
        }
        if (dto.sort() == null || dto.sort() < 0) {
            throw new BusinessException(400, "模块排序不能小于 0");
        }
        if (dto.enabled() == null || (dto.enabled() != ENABLED && dto.enabled() != 0)) {
            throw new BusinessException(400, "模块启用状态只能为 1（启用）或 0（停用）");
        }
        if (StrUtil.isNotBlank(dto.configJson()) && !JSONUtil.isTypeJSON(dto.configJson())) {
            throw new BusinessException(400, "模块参数必须是合法的 JSON");
        }
    }

    private SiteConfigVO toVO(Map<String, String> values) {
        return new SiteConfigVO(
                valueOrDefault(values, SiteConfigRegistry.SITE_NAME),
                valueOrDefault(values, SiteConfigRegistry.SITE_INTRO),
                valueOrDefault(values, SiteConfigRegistry.LOGO),
                valueOrDefault(values, SiteConfigRegistry.COPYRIGHT),
                valueOrDefault(values, SiteConfigRegistry.PRIMARY_COLOR),
                Boolean.parseBoolean(valueOrDefault(values, SiteConfigRegistry.DARK_MODE)));
    }

    /** 行缺失或为空时回落到注册表默认值，前端不必再判空 */
    private String valueOrDefault(Map<String, String> values, String key) {
        String value = values.get(key);
        return StrUtil.isBlank(value) ? SiteConfigRegistry.get(key).defaultValue() : value;
    }

    private HomeModuleVO toModuleVO(HomeModule module) {
        return new HomeModuleVO(
                module.getId(),
                module.getModuleType(),
                module.getModuleName(),
                module.getSort(),
                module.getEnabled(),
                module.getConfigJson());
    }
}
