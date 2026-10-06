package com.lelcc.service;

import com.lelcc.common.PageResult;
import com.lelcc.dto.TagDTO;
import com.lelcc.dto.TagVO;

import java.util.List;

public interface TagService {

    PageResult<TagVO> page(Long boardId, Integer tagType, Integer status, String keyword,
                           long pageNum, long pageSize);

    /** 用户端内容分类：启用的全局标签 + 该板块的标签；boardId 可空 */
    List<TagVO> listOpen(Long boardId);

    void create(TagDTO dto);

    void update(Long id, TagDTO dto);

    /** 设为正常(1)或停用(0) */
    void changeStatus(Long id, Integer status);

    /** 标签被帖子引用时不允许删除 */
    void delete(Long id);
}
