package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.BusinessException;
import com.lelcc.common.UiJsonUtils;
import com.lelcc.common.UiValueValidator;
import com.lelcc.dto.NodeDTO;
import com.lelcc.dto.NodeVO;
import com.lelcc.dto.PageDTO;
import com.lelcc.dto.PageTreeVO;
import com.lelcc.dto.PageVO;
import com.lelcc.entity.UiComponentType;
import com.lelcc.entity.UiNode;
import com.lelcc.entity.UiPage;
import com.lelcc.mapper.UiComponentTypeMapper;
import com.lelcc.mapper.UiNodeMapper;
import com.lelcc.mapper.UiPageMapper;
import com.lelcc.service.UiPageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UiPageServiceImpl implements UiPageService {

    private static final int STATUS_ENABLED = 1;
    private static final int VISIBLE = 1;
    private static final int IS_HOME = 1;
    private static final int CONTAINER = 1;

    @Resource
    private UiPageMapper pageMapper;

    @Resource
    private UiNodeMapper nodeMapper;

    @Resource
    private UiComponentTypeMapper componentTypeMapper;

    @Override
    public List<PageVO> list() {
        return pageMapper.selectList(Wrappers.<UiPage>lambdaQuery()
                        .orderByDesc(UiPage::getIsHome)
                        .orderByAsc(UiPage::getId))
                .stream()
                .map(this::toPageVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(PageDTO dto) {
        if (pageKeyExists(dto.pageKey(), null)) {
            throw new BusinessException(400, "页面标识已存在");
        }
        if (isHome(dto.isHome())) {
            clearHomeFlag(null);
        }
        UiPage page = new UiPage();
        page.setPageKey(dto.pageKey());
        page.setPageName(dto.pageName());
        page.setRoutePath(dto.routePath());
        page.setIsHome(isHome(dto.isHome()) ? IS_HOME : 0);
        page.setStatus(dto.status() == null ? STATUS_ENABLED : dto.status());
        page.setRemark(dto.remark());
        pageMapper.insert(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PageDTO dto) {
        UiPage exists = requirePage(id);
        if (pageKeyExists(dto.pageKey(), id)) {
            throw new BusinessException(400, "页面标识已存在");
        }
        if (isHome(dto.isHome())) {
            clearHomeFlag(id);
        }
        UiPage update = new UiPage();
        update.setId(exists.getId());
        update.setPageKey(dto.pageKey());
        update.setPageName(dto.pageName());
        update.setRoutePath(dto.routePath());
        update.setIsHome(isHome(dto.isHome()) ? IS_HOME : 0);
        update.setStatus(dto.status() == null ? exists.getStatus() : dto.status());
        update.setRemark(dto.remark());
        pageMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        UiPage page = requirePage(id);
        if (page.getIsHome() != null && page.getIsHome() == IS_HOME) {
            throw new BusinessException(400, "首页不可删除");
        }
        nodeMapper.delete(Wrappers.<UiNode>lambdaQuery().eq(UiNode::getPageId, id));
        pageMapper.deleteById(id);
    }

    @Override
    public List<NodeVO> listNodes(Long pageId) {
        requirePage(pageId);
        Map<String, UiComponentType> types = loadTypes();
        List<UiNode> nodes = loadNodes(pageId);
        Map<Long, NodeVO> voById = new LinkedHashMap<>();
        for (UiNode node : nodes) {
            voById.put(node.getId(), toNodeVO(node, types.get(node.getTypeCode()), false));
        }
        return buildTree(nodes, voById);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<NodeVO> saveNodes(Long pageId, List<NodeDTO> nodes) {
        requirePage(pageId);
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException(400, "页面节点不能为空");
        }
        Map<String, UiComponentType> types = loadTypes();
        List<UiNode> existing = loadNodes(pageId);
        Map<Long, UiNode> existingById = new HashMap<>();
        existing.forEach(node -> existingById.put(node.getId(), node));

        // 校验每个节点
        Set<Long> incomingIds = new HashSet<>();
        for (NodeDTO dto : nodes) {
            if (dto.id() != null && dto.id() > 0 && !existingById.containsKey(dto.id())) {
                throw new BusinessException(400, "存在不属于该页面的节点：" + dto.id());
            }
            UiComponentType type = types.get(dto.typeCode());
            if (type == null) {
                throw new BusinessException(400, "组件类型不存在：" + dto.typeCode());
            }
            String styleError = UiValueValidator.validate(
                    UiJsonUtils.toStringList(type.getStyleSchema()), dto.styleJson(), type.getTypeName() + " / 样式");
            if (styleError != null) {
                throw new BusinessException(400, styleError);
            }
            String contentError = UiValueValidator.validate(
                    UiJsonUtils.toStringList(type.getContentSchema()), dto.contentJson(), type.getTypeName() + " / 内容");
            if (contentError != null) {
                throw new BusinessException(400, contentError);
            }
            String behaviorError = UiValueValidator.validate(
                    UiJsonUtils.toStringList(type.getBehaviorSchema()), dto.behaviorJson(), type.getTypeName() + " / 行为");
            if (behaviorError != null) {
                throw new BusinessException(400, behaviorError);
            }
            if (dto.id() != null) {
                incomingIds.add(dto.id());
            }
        }

        Map<Long, String> typeCodeByNodeId = new HashMap<>();
        existingById.forEach((nid, node) -> typeCodeByNodeId.put(nid, node.getTypeCode()));
        for (NodeDTO dto : nodes) {
            if (dto.id() != null) {
                typeCodeByNodeId.put(dto.id(), dto.typeCode());
            }
        }

        // 先删掉本次未提交的旧节点
        for (Long existId : existingById.keySet()) {
            if (!incomingIds.contains(existId)) {
                nodeMapper.deleteById(existId);
            }
        }

        // 父节点必须先于子节点落库，才能拿到真实 id 回填 parent_id：
        // 逐轮处理"父已就绪"的节点，直到全部处理完；卡住说明存在环或悬空父节点
        Map<Long, Long> resolved = new HashMap<>();
        existingById.keySet().forEach(existId -> resolved.put(existId, existId));
        List<NodeDTO> pending = new ArrayList<>(nodes);
        while (!pending.isEmpty()) {
            int before = pending.size();
            pending.removeIf(dto -> {
                Long parentId = dto.parentId();
                Long persistedId;
                if (parentId == null) {
                    persistedId = persistNode(dto, pageId, null);
                } else {
                    Long realParent = resolved.get(parentId);
                    if (realParent == null) {
                        return false;
                    }
                    String parentTypeCode = typeCodeByNodeId.get(parentId);
                    UiComponentType parentType = parentTypeCode == null ? null : types.get(parentTypeCode);
                    if (parentType == null || parentType.getIsContainer() == null
                            || parentType.getIsContainer() != CONTAINER) {
                        throw new BusinessException(400, "父节点不是容器，不能容纳子节点");
                    }
                    persistedId = persistNode(dto, pageId, realParent);
                }
                if (dto.id() != null) {
                    resolved.put(dto.id(), persistedId);
                }
                return true;
            });
            if (pending.size() == before) {
                throw new BusinessException(400, "节点父子关系存在环或引用了不存在的父节点");
            }
        }

        // 把"临时 id -> 真实 id"回传，前端据此把动作里的目标改成真实 id
        Map<Long, Long> tempToReal = new HashMap<>();
        resolved.forEach((tempId, realId) -> {
            if (tempId < 0) {
                tempToReal.put(realId, tempId);
            }
        });
        List<NodeVO> tree = listNodes(pageId);
        if (!tempToReal.isEmpty()) {
            fillClientId(tree, tempToReal);
        }
        return tree;
    }

    /** 给本次新增的节点回填临时 id，供前端做动作目标回填 */
    private void fillClientId(List<NodeVO> nodes, Map<Long, Long> tempToReal) {
        for (NodeVO node : nodes) {
            node.setClientId(tempToReal.get(node.getId()));
            fillClientId(node.getChildren(), tempToReal);
        }
    }

    private Long persistNode(NodeDTO dto, Long pageId, Long parentId) {
        UiNode entity = new UiNode();
        boolean isNew = dto.id() == null || dto.id() < 0;
        entity.setPageId(pageId);
        entity.setParentId(parentId);
        entity.setTypeCode(dto.typeCode());
        entity.setNodeName(dto.nodeName());
        entity.setSort(dto.sort() == null ? 0 : dto.sort());
        entity.setVisible(dto.visible() == null ? VISIBLE : dto.visible());
        entity.setStyleJson(UiJsonUtils.toJson(dto.styleJson()));
        entity.setContentJson(UiJsonUtils.toJson(dto.contentJson()));
        entity.setBehaviorJson(UiJsonUtils.toJson(dto.behaviorJson()));
        entity.setRemark(dto.remark());
        if (isNew) {
            nodeMapper.insert(entity);
            return entity.getId();
        }
        entity.setId(dto.id());
        nodeMapper.updateById(entity);
        return dto.id();
    }

    @Override
    public PageTreeVO userTree(String pageKey) {
        UiPage page = pageMapper.selectOne(Wrappers.<UiPage>lambdaQuery()
                .eq(UiPage::getPageKey, pageKey)
                .eq(UiPage::getStatus, STATUS_ENABLED));
        if (page == null) {
            throw new BusinessException(404, "页面不存在或已停用");
        }
        Map<String, UiComponentType> types = loadTypes();
        List<UiNode> nodes = loadNodes(page.getId());
        Map<Long, UiNode> byId = new HashMap<>();
        nodes.forEach(node -> byId.put(node.getId(), node));

        Map<Long, NodeVO> voById = new LinkedHashMap<>();
        for (UiNode node : nodes) {
            if (node.getVisible() == null || node.getVisible() != VISIBLE) {
                continue;
            }
            if (!ancestorsVisible(node, byId)) {
                continue;
            }
            voById.put(node.getId(), toNodeVO(node, types.get(node.getTypeCode()), true));
        }
        List<UiNode> visibleNodes = nodes.stream().filter(node -> voById.containsKey(node.getId())).toList();

        PageTreeVO tree = new PageTreeVO();
        tree.setPageKey(page.getPageKey());
        tree.setPageName(page.getPageName());
        tree.setRoutePath(page.getRoutePath());
        tree.setNodes(buildTree(visibleNodes, voById));
        return tree;
    }

    private boolean ancestorsVisible(UiNode node, Map<Long, UiNode> byId) {
        Long parentId = node.getParentId();
        while (parentId != null) {
            UiNode parent = byId.get(parentId);
            if (parent == null || parent.getVisible() == null || parent.getVisible() != VISIBLE) {
                return false;
            }
            parentId = parent.getParentId();
        }
        return true;
    }

    /** 按 parent_id 把扁平节点组装成树，并对同级按 sort、id 排序 */
    private List<NodeVO> buildTree(List<UiNode> nodes, Map<Long, NodeVO> voById) {
        List<NodeVO> roots = new ArrayList<>();
        for (UiNode node : nodes) {
            NodeVO vo = voById.get(node.getId());
            if (vo == null) {
                continue;
            }
            Long parentId = node.getParentId();
            NodeVO parent = parentId == null ? null : voById.get(parentId);
            if (parent == null) {
                roots.add(vo);
            } else {
                parent.getChildren().add(vo);
            }
        }
        Comparator<NodeVO> comparator = Comparator
                .comparing((NodeVO vo) -> vo.getSort() == null ? 0 : vo.getSort())
                .thenComparing(vo -> vo.getId() == null ? 0 : vo.getId());
        sortRecursively(roots, comparator);
        return roots;
    }

    private void sortRecursively(List<NodeVO> nodes, Comparator<NodeVO> comparator) {
        nodes.sort(comparator);
        for (NodeVO node : nodes) {
            sortRecursively(node.getChildren(), comparator);
        }
    }

    private NodeVO toNodeVO(UiNode node, UiComponentType type, boolean mergeDefaults) {
        Map<String, Object> style = UiJsonUtils.toMap(node.getStyleJson());
        Map<String, Object> content = UiJsonUtils.toMap(node.getContentJson());
        Map<String, Object> behavior = UiJsonUtils.toMap(node.getBehaviorJson());
        if (mergeDefaults && type != null) {
            style = UiJsonUtils.merge(UiJsonUtils.toMap(type.getDefaultStyle()), style);
            content = UiJsonUtils.merge(UiJsonUtils.toMap(type.getDefaultContent()), content);
        }
        NodeVO vo = new NodeVO();
        vo.setId(node.getId());
        vo.setParentId(node.getParentId());
        vo.setTypeCode(node.getTypeCode());
        vo.setNodeName(node.getNodeName());
        vo.setSort(node.getSort());
        vo.setVisible(node.getVisible());
        vo.setStyle(style);
        vo.setContent(content);
        vo.setBehavior(behavior);
        vo.setRemark(node.getRemark());
        if (type != null) {
            vo.setTypeName(type.getTypeName());
            vo.setRenderKind(type.getRenderKind());
            vo.setIsContainer(type.getIsContainer());
            vo.setIsBuiltin(type.getIsBuiltin());
        }
        return vo;
    }

    private PageVO toPageVO(UiPage page) {
        PageVO vo = new PageVO();
        vo.setId(page.getId());
        vo.setPageKey(page.getPageKey());
        vo.setPageName(page.getPageName());
        vo.setRoutePath(page.getRoutePath());
        vo.setIsHome(page.getIsHome());
        vo.setStatus(page.getStatus());
        vo.setRemark(page.getRemark());
        vo.setCreateTime(page.getCreateTime());
        vo.setUpdateTime(page.getUpdateTime());
        vo.setNodeCount(Math.toIntExact(nodeMapper.selectCount(
                Wrappers.<UiNode>lambdaQuery().eq(UiNode::getPageId, page.getId()))));
        return vo;
    }

    private Map<String, UiComponentType> loadTypes() {
        Map<String, UiComponentType> types = new HashMap<>();
        for (UiComponentType type : componentTypeMapper.selectList(null)) {
            types.put(type.getTypeCode(), type);
        }
        return types;
    }

    private List<UiNode> loadNodes(Long pageId) {
        return nodeMapper.selectList(Wrappers.<UiNode>lambdaQuery()
                .eq(UiNode::getPageId, pageId)
                .orderByAsc(UiNode::getSort)
                .orderByAsc(UiNode::getId));
    }

    private void clearHomeFlag(Long excludeId) {
        pageMapper.update(null, Wrappers.<UiPage>lambdaUpdate()
                .set(UiPage::getIsHome, 0)
                .ne(excludeId != null, UiPage::getId, excludeId));
    }

    private UiPage requirePage(Long id) {
        UiPage page = pageMapper.selectById(id);
        if (page == null) {
            throw new BusinessException(404, "页面不存在");
        }
        return page;
    }

    private boolean pageKeyExists(String pageKey, Long excludeId) {
        return pageMapper.exists(Wrappers.<UiPage>lambdaQuery()
                .eq(UiPage::getPageKey, pageKey)
                .ne(excludeId != null, UiPage::getId, excludeId));
    }

    private static boolean isHome(Integer value) {
        return value != null && value == IS_HOME;
    }
}
