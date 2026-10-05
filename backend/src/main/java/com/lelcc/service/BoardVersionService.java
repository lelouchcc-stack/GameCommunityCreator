package com.lelcc.service;

import com.lelcc.dto.BoardVersionDTO;
import com.lelcc.dto.BoardVersionVO;

import java.util.List;

public interface BoardVersionService {

    /** 某板块的全部版本，按发布时间倒序；isCurrent 由 board.current_version_id 动态判定 */
    List<BoardVersionVO> listByBoard(Long boardId);

    void create(Long boardId, BoardVersionDTO dto);

    void update(Long boardId, Long id, BoardVersionDTO dto);

    /** 设为当前版本：同一事务内更新 board.current_version_id */
    void setCurrent(Long boardId, Long id);

    /** 设为正常(1)或停用(0) */
    void changeStatus(Long boardId, Long id, Integer status);

    /** 版本被帖子引用或为当前版本时不允许删除 */
    void delete(Long boardId, Long id);
}
