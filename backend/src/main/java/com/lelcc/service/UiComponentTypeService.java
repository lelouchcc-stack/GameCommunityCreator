package com.lelcc.service;

import com.lelcc.dto.ComponentTypeDTO;
import com.lelcc.dto.ComponentTypeVO;

import java.util.List;

/** 组件库（组件类型）管理。 */
public interface UiComponentTypeService {

    List<ComponentTypeVO> list(Integer status);

    void create(ComponentTypeDTO dto);

    void update(Long id, ComponentTypeDTO dto);

    void delete(Long id);
}
