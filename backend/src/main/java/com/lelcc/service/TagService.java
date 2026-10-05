package com.lelcc.service;

import com.lelcc.common.PageResult;
import com.lelcc.dto.TagDTO;
import com.lelcc.dto.TagVO;

public interface TagService {

    PageResult<TagVO> page(Long boardId, Integer tagType, Integer status, String keyword,
                           long pageNum, long pageSize);

    void create(TagDTO dto);

    void update(Long id, TagDTO dto);

    /** 设为正常(1)或停用(0) */
    void changeStatus(Long id, Integer status);

    /** 标签被帖子引用时不允许删除 */
    void delete(Long id);
}
