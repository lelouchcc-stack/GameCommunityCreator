package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.UiJsonUtils;
import com.lelcc.dto.LogicNodeVO;
import com.lelcc.entity.UiLogicNodeType;
import com.lelcc.mapper.UiLogicNodeTypeMapper;
import com.lelcc.service.UiLogicNodeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UiLogicNodeServiceImpl implements UiLogicNodeService {

    @Resource
    private UiLogicNodeTypeMapper logicNodeMapper;

    @Override
    public List<LogicNodeVO> list(Integer status) {
        return logicNodeMapper.selectList(Wrappers.<UiLogicNodeType>lambdaQuery()
                        .eq(status != null, UiLogicNodeType::getStatus, status)
                        .orderByAsc(UiLogicNodeType::getSort)
                        .orderByAsc(UiLogicNodeType::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private LogicNodeVO toVO(UiLogicNodeType entity) {
        LogicNodeVO vo = new LogicNodeVO();
        vo.setId(entity.getId());
        vo.setTypeCode(entity.getTypeCode());
        vo.setTypeName(entity.getTypeName());
        vo.setKind(entity.getKind());
        vo.setCategory(entity.getCategory());
        vo.setRunner(entity.getRunner());
        vo.setSourceApi(entity.getSourceApi());
        vo.setInputs(UiJsonUtils.toMapList(entity.getInputsSchema()));
        vo.setOutputs(UiJsonUtils.toMapList(entity.getOutputsSchema()));
        vo.setConfig(UiJsonUtils.toMapList(entity.getConfigSchema()));
        vo.setPreviewIcon(entity.getPreviewIcon());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
