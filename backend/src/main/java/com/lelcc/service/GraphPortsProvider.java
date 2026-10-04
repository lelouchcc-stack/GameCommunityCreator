package com.lelcc.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.GraphValidator;
import com.lelcc.common.UiJsonUtils;
import com.lelcc.entity.UiLogicNodeType;
import com.lelcc.entity.UiPreset;
import com.lelcc.mapper.UiLogicNodeTypeMapper;
import com.lelcc.mapper.UiPresetMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 把原子库与预设库的 schema 汇成"节点类型 -> 端口集合"的表，供 {@link GraphValidator} 使用。
 * 只收 status=1 的：停用的原子/预设不允许再出现在图里。
 */
@Component
public class GraphPortsProvider {

    private static final int STATUS_ENABLED = 1;

    @Resource
    private UiLogicNodeTypeMapper logicNodeMapper;

    @Resource
    private UiPresetMapper presetMapper;

    public Map<String, GraphValidator.Ports> portsByKey() {
        Map<String, GraphValidator.Ports> ports = new LinkedHashMap<>();
        logicNodeMapper.selectList(Wrappers.<UiLogicNodeType>lambdaQuery()
                        .eq(UiLogicNodeType::getStatus, STATUS_ENABLED))
                .forEach(atom -> ports.put(atom.getTypeCode(), GraphValidator.atomPorts(
                        atom.getKind(), atom.getTypeCode(),
                        UiJsonUtils.toMapList(atom.getInputsSchema()),
                        UiJsonUtils.toMapList(atom.getOutputsSchema()),
                        UiJsonUtils.toMapList(atom.getConfigSchema()))));
        presetMapper.selectList(Wrappers.<UiPreset>lambdaQuery().eq(UiPreset::getStatus, STATUS_ENABLED))
                .forEach(preset -> ports.put(
                        GraphValidator.presetKey(preset.getPresetCode(), preset.getVersion()),
                        GraphValidator.presetPorts(
                                UiJsonUtils.toMapList(preset.getInputsSchema()),
                                UiJsonUtils.toMapList(preset.getOutputsSchema()))));
        return ports;
    }

    /**
     * 原子 typeCode -> nodeRef 型设置项的 key。
     * 这些键的值必须指向**本页的 ui_node**：页面图能绑组件，跨页面的预设不能绑。
     */
    public Map<String, Set<String>> nodeRefKeys() {
        Map<String, Set<String>> keys = new LinkedHashMap<>();
        logicNodeMapper.selectList(Wrappers.<UiLogicNodeType>lambdaQuery()
                        .eq(UiLogicNodeType::getStatus, STATUS_ENABLED))
                .forEach(atom -> {
                    Set<String> refs = new LinkedHashSet<>();
                    for (Map<String, Object> item : UiJsonUtils.toMapList(atom.getConfigSchema())) {
                        if ("nodeRef".equals(String.valueOf(item.get("type")))) {
                            refs.add(String.valueOf(item.get("key")));
                        }
                    }
                    if (!refs.isEmpty()) {
                        keys.put(atom.getTypeCode(), refs);
                    }
                });
        return keys;
    }
}
