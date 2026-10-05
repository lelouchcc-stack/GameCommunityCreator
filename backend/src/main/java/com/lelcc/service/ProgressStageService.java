package com.lelcc.service;

import com.lelcc.dto.ProgressStageDTO;
import com.lelcc.dto.ProgressStageVO;
import com.lelcc.dto.StageSortDTO;

import java.util.List;

public interface ProgressStageService {

    /** 某板块的进度阶段，按 sort 排序 */
    List<ProgressStageVO> listByBoard(Long boardId);

    void create(Long boardId, ProgressStageDTO dto);

    void update(Long boardId, Long id, ProgressStageDTO dto);

    /** 批量调整顺序 */
    void reorder(Long boardId, List<StageSortDTO> items);

    /** 设为正常(1)或停用(0) */
    void changeStatus(Long boardId, Long id, Integer status);

    /** 阶段被帖子或玩家进度引用时不允许删除 */
    void delete(Long boardId, Long id);
}
