package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.BusinessException;
import com.lelcc.common.GraphValidator;
import com.lelcc.common.UiJsonUtils;
import com.lelcc.dto.FlowDTO;
import com.lelcc.dto.FlowVO;
import com.lelcc.entity.UiFlow;
import com.lelcc.entity.UiNode;
import com.lelcc.entity.UiPage;
import com.lelcc.mapper.UiFlowMapper;
import com.lelcc.mapper.UiNodeMapper;
import com.lelcc.mapper.UiPageMapper;
import com.lelcc.service.GraphPortsProvider;
import com.lelcc.service.UiFlowService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UiFlowServiceImpl implements UiFlowService {

    private static final int STATUS_ENABLED = 1;
    private static final Pattern FLOW_KEY = Pattern.compile("^[a-z][a-z0-9_-]{0,29}$");

    @Resource
    private UiFlowMapper flowMapper;

    @Resource
    private UiPageMapper pageMapper;

    @Resource
    private UiNodeMapper nodeMapper;

    @Resource
    private GraphPortsProvider portsProvider;

    @Override
    public List<FlowVO> listByPage(Long pageId) {
        requirePage(pageId);
        return flowMapper.selectList(Wrappers.<UiFlow>lambdaQuery()
                        .eq(UiFlow::getPageId, pageId)
                        .orderByAsc(UiFlow::getFlowKey))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Long pageId, String flowKey, FlowDTO dto) {
        requirePage(pageId);
        if (flowKey == null || !FLOW_KEY.matcher(flowKey).matches()) {
            throw new BusinessException(400, "图标识要小写字母开头，后接小写字母/数字/下划线/短横线，最长 30 位");
        }
        // 图级校验（页面图没有预设的入参/出参声明，所以后两个参数传 null）
        String graphError = GraphValidator.validate(dto.graphJson(), portsProvider.portsByKey(), null, null);
        if (graphError != null) {
            throw new BusinessException(400, graphError);
        }
        checkNodeRefs(pageId, dto.graphJson());

        UiFlow exists = flowMapper.selectOne(Wrappers.<UiFlow>lambdaQuery()
                .eq(UiFlow::getPageId, pageId)
                .eq(UiFlow::getFlowKey, flowKey));
        UiFlow entity = new UiFlow();
        entity.setPageId(pageId);
        entity.setFlowKey(flowKey);
        entity.setFlowName(dto.flowName());
        entity.setGraphJson(UiJsonUtils.toJson(dto.graphJson()));
        entity.setStatus(dto.status() == null ? STATUS_ENABLED : dto.status());
        entity.setRemark(dto.remark());
        if (exists == null) {
            flowMapper.insert(entity);
        } else {
            entity.setId(exists.getId());
            flowMapper.updateById(entity);
        }
    }

    /**
     * nodeRef 型设置项（on.click/on.submit 的 nodeId、act.openModal/act.setVisible 的 nodeId…）
     * 必须指向**本页**的组件：图与组件树是同一页的两半，指到别的页面就是悬空引用。
     */
    private void checkNodeRefs(Long pageId, Map<String, Object> graph) {
        Object nodesRaw = graph == null ? null : graph.get("nodes");
        if (!(nodesRaw instanceof List<?> nodes)) {
            return;
        }
        Map<String, Set<String>> nodeRefKeys = portsProvider.nodeRefKeys();
        Set<Long> pageNodeIds = nodeMapper.selectList(Wrappers.<UiNode>lambdaQuery().eq(UiNode::getPageId, pageId))
                .stream()
                .map(UiNode::getId)
                .collect(Collectors.toSet());
        for (Object item : nodes) {
            if (!(item instanceof Map<?, ?> node)) {
                continue;
            }
            Set<String> keys = nodeRefKeys.get(String.valueOf(node.get("typeCode")));
            if (keys == null || !(node.get("config") instanceof Map<?, ?> config)) {
                continue;
            }
            for (String key : keys) {
                Object value = config.get(key);
                if (value == null || String.valueOf(value).isBlank()) {
                    continue;
                }
                Long nodeId = parseId(value);
                if (nodeId == null || !pageNodeIds.contains(nodeId)) {
                    throw new BusinessException(400, "节点 " + node.get("id") + " 的 " + key
                            + " 指向的不是本页组件：" + value);
                }
            }
        }
    }

    private Long parseId(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void requirePage(Long pageId) {
        UiPage page = pageId == null ? null : pageMapper.selectById(pageId);
        if (page == null) {
            throw new BusinessException(404, "页面不存在");
        }
    }

    private FlowVO toVO(UiFlow entity) {
        FlowVO vo = new FlowVO();
        vo.setId(entity.getId());
        vo.setPageId(entity.getPageId());
        vo.setFlowKey(entity.getFlowKey());
        vo.setFlowName(entity.getFlowName());
        vo.setGraph(UiJsonUtils.toMap(entity.getGraphJson()));
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
