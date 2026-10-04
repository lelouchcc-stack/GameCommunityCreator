package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.BusinessException;
import com.lelcc.common.GraphValidator;
import com.lelcc.common.PortTypes;
import com.lelcc.common.UiJsonUtils;
import com.lelcc.dto.PresetDTO;
import com.lelcc.dto.PresetVO;
import com.lelcc.entity.UiFlow;
import com.lelcc.entity.UiPreset;
import com.lelcc.mapper.UiFlowMapper;
import com.lelcc.mapper.UiPresetMapper;
import com.lelcc.service.GraphPortsProvider;
import com.lelcc.service.UiPresetService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class UiPresetServiceImpl implements UiPresetService {

    private static final int STATUS_ENABLED = 1;
    private static final int BUILTIN = 1;
    private static final String CUSTOM_CODE_BASE = "preset.custom";

    @Resource
    private UiPresetMapper presetMapper;

    @Resource
    private UiFlowMapper flowMapper;

    @Resource
    private GraphPortsProvider portsProvider;

    @Override
    public List<PresetVO> list(Integer status) {
        return presetMapper.selectList(Wrappers.<UiPreset>lambdaQuery()
                        .eq(status != null, UiPreset::getStatus, status)
                        .orderByAsc(UiPreset::getPresetCode)
                        .orderByDesc(UiPreset::getVersion))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public void create(PresetDTO dto) {
        validate(dto);
        UiPreset entity = new UiPreset();
        entity.setPresetCode(generateCustomCode());
        entity.setPresetName(dto.presetName());
        entity.setVersion(1);
        entity.setIsBuiltin(0);
        apply(entity, dto);
        presetMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PresetDTO dto) {
        UiPreset exists = requirePreset(id);
        if (isBuiltin(exists)) {
            throw new BusinessException(400, "内置预设只读，请先『复制为自定义』再改");
        }
        validate(dto);
        if (referenced(exists.getPresetCode(), exists.getVersion())) {
            // 已被引用：不原地改旧版本，另插一个新版本，引用方仍锁在旧版本上
            UiPreset next = new UiPreset();
            next.setPresetCode(exists.getPresetCode());
            next.setPresetName(dto.presetName());
            next.setVersion(nextVersion(exists.getPresetCode()));
            next.setIsBuiltin(0);
            apply(next, dto);
            presetMapper.insert(next);
            return;
        }
        UiPreset update = new UiPreset();
        update.setId(exists.getId());
        update.setPresetName(dto.presetName());
        apply(update, dto);
        presetMapper.updateById(update);
    }

    @Override
    public void delete(Long id) {
        UiPreset exists = requirePreset(id);
        if (isBuiltin(exists)) {
            throw new BusinessException(400, "内置预设不可删除，可改为停用");
        }
        if (referenced(exists.getPresetCode(), exists.getVersion())) {
            throw new BusinessException(400, "该预设已被页面逻辑图引用，请先解除引用");
        }
        presetMapper.deleteById(id);
    }

    private void apply(UiPreset entity, PresetDTO dto) {
        entity.setCategory(dto.category());
        entity.setInputsSchema(UiJsonUtils.toJson(nullToEmpty(dto.inputsSchema())));
        entity.setOutputsSchema(UiJsonUtils.toJson(nullToEmpty(dto.outputsSchema())));
        entity.setGraphJson(UiJsonUtils.toJson(dto.graphJson()));
        entity.setStatus(dto.status() == null ? STATUS_ENABLED : dto.status());
        entity.setRemark(dto.remark());
    }

    private void validate(PresetDTO dto) {
        List<Map<String, Object>> inputs = nullToEmpty(dto.inputsSchema());
        List<Map<String, Object>> outputs = nullToEmpty(dto.outputsSchema());
        checkSchema(inputs, "入参");
        checkSchema(outputs, "出参");

        List<String> params = inputs.stream().map(item -> String.valueOf(item.get("key"))).toList();
        Map<String, String> declaredOutputs = new LinkedHashMap<>();
        for (Map<String, Object> item : outputs) {
            declaredOutputs.put(String.valueOf(item.get("key")), String.valueOf(item.get("type")));
        }
        String graphError = GraphValidator.validate(dto.graphJson(), portsProvider.portsByKey(), params, declaredOutputs);
        if (graphError != null) {
            throw new BusinessException(400, graphError);
        }

        // 声明的出参与图的 exports 必须一一对应，否则预设节点上会有接不出值的端口
        Set<String> declared = new LinkedHashSet<>(declaredOutputs.keySet());
        Object exportsRaw = dto.graphJson() == null ? null : dto.graphJson().get("exports");
        Set<String> exported = new LinkedHashSet<>();
        if (exportsRaw instanceof Map<?, ?> exports) {
            exports.keySet().forEach(key -> exported.add(String.valueOf(key)));
        }
        if (!declared.equals(exported)) {
            throw new BusinessException(400, "出参声明与图的 exports 不一致：声明 " + declared + " / 导出 " + exported);
        }
    }

    private void checkSchema(List<Map<String, Object>> schema, String label) {
        Set<String> keys = new HashSet<>();
        for (Map<String, Object> item : schema) {
            Object key = item.get("key");
            Object type = item.get("type");
            if (key == null || type == null) {
                throw new BusinessException(400, label + "每一项都要有 key 与 type");
            }
            if (!keys.add(String.valueOf(key))) {
                throw new BusinessException(400, label + "存在重复的 key：" + key);
            }
            if (!PortTypes.isKnown(String.valueOf(type))) {
                throw new BusinessException(400, label + "的端口类型未注册：" + type);
            }
        }
    }

    /**
     * 预设是否被页面逻辑图引用。图以 JSON 存在 ui_flow.graph_json，这里做字符串匹配
     * （服务端序列化固定为紧凑 JSON，所以 "presetCode":"x" 是稳定子串）。
     * 这是守卫性检查而非引用表：误判只会让改动退化成"新建版本"，不会损坏数据。
     */
    private boolean referenced(String presetCode, Integer version) {
        return flowMapper.exists(Wrappers.<UiFlow>lambdaQuery()
                .like(UiFlow::getGraphJson, "\"presetCode\":\"" + presetCode + "\"")
                .like(UiFlow::getGraphJson, "\"presetVersion\":" + version));
    }

    private int nextVersion(String presetCode) {
        Integer max = presetMapper.selectList(Wrappers.<UiPreset>lambdaQuery()
                        .eq(UiPreset::getPresetCode, presetCode))
                .stream()
                .map(UiPreset::getVersion)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        return max + 1;
    }

    private String generateCustomCode() {
        String candidate = CUSTOM_CODE_BASE;
        int suffix = 1;
        while (codeExists(candidate)) {
            candidate = CUSTOM_CODE_BASE + "_" + (++suffix);
        }
        return candidate;
    }

    private boolean codeExists(String presetCode) {
        return presetMapper.exists(Wrappers.<UiPreset>lambdaQuery().eq(UiPreset::getPresetCode, presetCode));
    }

    private UiPreset requirePreset(Long id) {
        UiPreset preset = presetMapper.selectById(id);
        if (preset == null) {
            throw new BusinessException(404, "预设不存在");
        }
        return preset;
    }

    private boolean isBuiltin(UiPreset preset) {
        return preset.getIsBuiltin() != null && preset.getIsBuiltin() == BUILTIN;
    }

    private PresetVO toVO(UiPreset entity) {
        PresetVO vo = new PresetVO();
        vo.setId(entity.getId());
        vo.setPresetCode(entity.getPresetCode());
        vo.setPresetName(entity.getPresetName());
        vo.setVersion(entity.getVersion());
        vo.setIsBuiltin(entity.getIsBuiltin());
        vo.setCategory(entity.getCategory());
        vo.setInputs(UiJsonUtils.toMapList(entity.getInputsSchema()));
        vo.setOutputs(UiJsonUtils.toMapList(entity.getOutputsSchema()));
        vo.setGraph(UiJsonUtils.toMap(entity.getGraphJson()));
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private static List<Map<String, Object>> nullToEmpty(List<Map<String, Object>> list) {
        return list == null ? List.of() : list;
    }
}
