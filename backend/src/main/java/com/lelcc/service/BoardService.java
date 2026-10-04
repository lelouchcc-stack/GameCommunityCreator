package com.lelcc.service;

import com.lelcc.common.PageResult;
import com.lelcc.dto.BoardDTO;
import com.lelcc.dto.BoardVO;

import java.util.List;

public interface BoardService {

    PageResult<BoardVO> page(String name, Integer status, long pageNum, long pageSize);

    /** 用户端板块导航：全部开放板块，按 sort 排序 */
    List<BoardVO> listOpen();

    void create(BoardDTO dto);

    void update(Long id, BoardDTO dto);

    /** 设为开放(1)或关闭(0) */
    void changeStatus(Long id, Integer status);

    /** 删除前会校验板块下是否还有帖子 */
    void delete(Long id);
}
