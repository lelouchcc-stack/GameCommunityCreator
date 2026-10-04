package com.lelcc.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.BusinessException;
import com.lelcc.common.ComponentPropertyRegistry;
import com.lelcc.common.UiJsonUtils;
import com.lelcc.common.UiValueValidator;
import com.lelcc.dto.ComponentTypeDTO;
import com.lelcc.dto.ComponentTypeVO;
import com.lelcc.entity.UiComponentType;
import com.lelcc.entity.UiNode;
import com.lelcc.mapper.UiComponentTypeMapper;
import com.lelcc.mapper.UiNodeMapper;
import com.lelcc.service.UiComponentTypeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class UiComponentTypeServiceImpl implements UiComponentTypeService {

    private static final int STATUS_ENABLED = 1;
    private static final int BUILTIN = 1;

    /** 已注册的渲染内核；新增内核属于开发工作（设计里的 L4/EXT 边界） */
    private static final Set<String> RENDER_KINDS = Set.of(
            "text", "heading", "image", "button", "divider", "spacer",
            "container", "carousel", "list", "tabs", "modal", "icon");

    private static final Set<String> CATEGORIES = Set.of(
            "base", "layout", "display", "interactive", "business");

    @Resource
    private UiComponentTypeMapper componentTypeMapper;

    @Resource
    private UiNodeMapper nodeMapper;

    @Override
    public List<ComponentTypeVO> list(Integer status) {
        return componentTypeMapper.selectList(Wrappers.<UiComponentType>lambdaQuery()
                        .eq(status != null, UiComponentType::getStatus, status)
                        .orderByAsc(UiComponentType::getSort)
                        .orderByAsc(UiComponentType::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public void create(ComponentTypeDTO dto) {
        validate(dto);
        UiComponentType entity = new UiComponentType();
        entity.setTypeCode(generateCode(dto.renderKind()));
        entity.setTypeName(dto.typeName());
        entity.setCategory(dto.category());
        entity.setRenderKind(dto.renderKind());
        entity.setIsContainer(intFlag(dto.isContainer()));
        // 管理端新增的都是展示型；业务组件由代码/种子提供
        entity.setIsBuiltin(0);
        applyEditable(entity, dto);
        entity.setStatus(STATUS_ENABLED);
        componentTypeMapper.insert(entity);
    }

    @Override
    public void update(Long id, ComponentTypeDTO dto) {
        UiComponentType exists = requireType(id);
        validate(dto);
        UiComponentType update = new UiComponentType();
        update.setId(exists.getId());
        update.setTypeName(dto.typeName());
        update.setCategory(dto.category());
        update.setRenderKind(dto.renderKind());
        update.setIsContainer(intFlag(dto.isContainer()));
        applyEditable(update, dto);
        componentTypeMapper.updateById(update);
    }

    @Override
    public void delete(Long id) {
        UiComponentType exists = requireType(id);
        if (exists.getIsBuiltin() != null && exists.getIsBuiltin() == BUILTIN) {
            throw new BusinessException(400, "业务组件不可删除，可改为停用");
        }
        boolean used = nodeMapper.exists(Wrappers.<UiNode>lambdaQuery().eq(UiNode::getTypeCode, exists.getTypeCode()));
        if (used) {
            throw new BusinessException(400, "该组件已被页面使用，请先移除相关节点或改为停用");
        }
        componentTypeMapper.deleteById(id);
    }

    private void applyEditable(UiComponentType entity, ComponentTypeDTO dto) {
        entity.setStyleSchema(UiJsonUtils.toJson(dto.styleSchema()));
        entity.setContentSchema(UiJsonUtils.toJson(dto.contentSchema()));
        entity.setBehaviorSchema(UiJsonUtils.toJson(dto.behaviorSchema()));
        entity.setDefaultStyle(UiJsonUtils.toJson(dto.defaultStyle()));
        entity.setDefaultContent(UiJsonUtils.toJson(dto.defaultContent()));
        entity.setPreviewIcon(dto.previewIcon());
        entity.setSort(dto.sort() == null ? 0 : dto.sort());
        entity.setRemark(dto.remark());
    }

    private void validate(ComponentTypeDTO dto) {
        if (!CATEGORIES.contains(dto.category())) {
            throw new BusinessException(400, "组件分类不合法");
        }
        String renderKind = dto.renderKind();
        if (renderKind.startsWith("builtin:")) {
            throw new BusinessException(400, "业务组件由系统提供，不能通过此处新增");
        }
        if (!RENDER_KINDS.contains(renderKind)) {
            throw new BusinessException(400, "渲染内核未注册：" + renderKind);
        }
        List<String> styleSchema = nullToEmpty(dto.styleSchema());
        List<String> contentSchema = nullToEmpty(dto.contentSchema());
        List<String> behaviorSchema = nullToEmpty(dto.behaviorSchema());
        checkSchema(styleSchema, "样式");
        checkSchema(contentSchema, "内容");
        checkSchema(behaviorSchema, "行为");

        String styleError = UiValueValidator.validate(styleSchema, dto.defaultStyle(), "默认样式");
        if (styleError != null) {
            throw new BusinessException(400, styleError);
        }
        String contentError = UiValueValidator.validate(contentSchema, dto.defaultContent(), "默认内容");
        if (contentError != null) {
            throw new BusinessException(400, contentError);
        }
    }

    private void checkSchema(List<String> schema, String label) {
        for (String key : schema) {
            if (!ComponentPropertyRegistry.isKnown(key)) {
                throw new BusinessException(400, label + "属性不在属性库中：" + key);
            }
        }
    }

    private String generateCode(String renderKind) {
        String base = renderKind.replace("builtin:", "");
        String candidate = base;
        int suffix = 1;
        while (typeCodeExists(candidate)) {
            candidate = base + "_" + (++suffix);
        }
        return candidate;
    }

    private boolean typeCodeExists(String typeCode) {
        return componentTypeMapper.exists(Wrappers.<UiComponentType>lambdaQuery()
                .eq(UiComponentType::getTypeCode, typeCode));
    }

    private UiComponentType requireType(Long id) {
        UiComponentType type = componentTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(404, "组件类型不存在");
        }
        return type;
    }

    private ComponentTypeVO toVO(UiComponentType entity) {
        ComponentTypeVO vo = new ComponentTypeVO();
        vo.setId(entity.getId());
        vo.setTypeCode(entity.getTypeCode());
        vo.setTypeName(entity.getTypeName());
        vo.setCategory(entity.getCategory());
        vo.setRenderKind(entity.getRenderKind());
        vo.setIsContainer(entity.getIsContainer());
        vo.setIsBuiltin(entity.getIsBuiltin());
        vo.setStyleSchema(UiJsonUtils.toStringList(entity.getStyleSchema()));
        vo.setContentSchema(UiJsonUtils.toStringList(entity.getContentSchema()));
        vo.setBehaviorSchema(UiJsonUtils.toStringList(entity.getBehaviorSchema()));
        vo.setDefaultStyle(UiJsonUtils.toMap(entity.getDefaultStyle()));
        vo.setDefaultContent(UiJsonUtils.toMap(entity.getDefaultContent()));
        vo.setPreviewIcon(entity.getPreviewIcon());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private static int intFlag(Integer value) {
        return value == null ? 0 : value;
    }

    private static List<String> nullToEmpty(List<String> list) {
        return list == null ? List.of() : list;
    }
}
