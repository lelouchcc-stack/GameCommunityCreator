package com.lelcc.service;

import com.lelcc.dto.PresetDTO;
import com.lelcc.dto.PresetVO;

import java.util.List;

/** 预设库（原子团）：内置只读可复制，自定义可增删改。 */
public interface UiPresetService {

    List<PresetVO> list(Integer status);

    void create(PresetDTO dto);

    void update(Long id, PresetDTO dto);

    void delete(Long id);
}
