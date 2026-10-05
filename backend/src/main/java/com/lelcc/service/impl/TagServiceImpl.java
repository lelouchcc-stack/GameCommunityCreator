package com.lelcc.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lelcc.common.BusinessException;
import com.lelcc.common.PageResult;
import com.lelcc.dto.TagDTO;
import com.lelcc.dto.TagVO;
import com.lelcc.entity.Tag;
import com.lelcc.mapper.BoardMapper;
import com.lelcc.mapper.TagMapper;
import com.lelcc.service.TagService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TagServiceImpl implements TagService {

    private static final int STATUS_NORMAL = 1;
    private static final int STATUS_DISABLED = 0;
    private static final int DEFAULT_SORT = 0;

    @Resource
    private TagMapper tagMapper;

    @Resource
    private BoardMapper boardMapper;

    @Override
    public PageResult<TagVO> page(Long boardId, Integer tagType, Integer status, String keyword,
                                  long pageNum, long pageSize) {
        Page<TagVO> page = new Page<>(pageNum, pageSize);
        return PageResult.of(tagMapper.selectTagPage(page, boardId, tagType, status, keyword));
    }

    @Override
    public void create(TagDTO dto) {
        requireBoardIfPresent(dto.boardId());
        if (tagNameExists(dto.boardId(), dto.tagName(), null)) {
            throw new BusinessException(400, "标签名已存在，请修改");
        }
        Tag tag = new Tag();
        tag.setBoardId(dto.boardId());
        tag.setTagName(dto.tagName());
        tag.setTagType(dto.tagType());
        tag.setSort(dto.sort() == null ? DEFAULT_SORT : dto.sort());
        tag.setStatus(STATUS_NORMAL);
        tagMapper.insert(tag);
    }

    @Override
    public void update(Long id, TagDTO dto) {
        requireTag(id);
        requireBoardIfPresent(dto.boardId());
        if (tagNameExists(dto.boardId(), dto.tagName(), id)) {
            throw new BusinessException(400, "标签名已存在，请修改");
        }
        Tag tag = new Tag();
        tag.setId(id);
        tag.setBoardId(dto.boardId());
        tag.setTagName(dto.tagName());
        tag.setTagType(dto.tagType());
        tag.setSort(dto.sort() == null ? DEFAULT_SORT : dto.sort());
        tagMapper.updateById(tag);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_NORMAL && status != STATUS_DISABLED)) {
            throw new BusinessException(400, "状态只能为 1（正常）或 0（停用）");
        }
        requireTag(id);
        Tag update = new Tag();
        update.setId(id);
        update.setStatus(status);
        tagMapper.updateById(update);
    }

    @Override
    public void delete(Long id) {
        requireTag(id);
        long refs = tagMapper.countPostRefs(id);
        if (refs > 0) {
            throw new BusinessException(400,
                    "该标签已被 " + refs + " 篇帖子引用，无法删除，请改为停用");
        }
        tagMapper.deleteById(id);
    }

    private void requireBoardIfPresent(Long boardId) {
        if (boardId != null && boardMapper.selectById(boardId) == null) {
            throw new BusinessException(404, "所属板块不存在");
        }
    }

    private Tag requireTag(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }
        return tag;
    }

    /** 同一板块（含全局 boardId=null）下标签名唯一 */
    private boolean tagNameExists(Long boardId, String tagName, Long excludeId) {
        return tagMapper.exists(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getTagName, tagName)
                .isNull(boardId == null, Tag::getBoardId)
                .eq(boardId != null, Tag::getBoardId, boardId)
                .ne(excludeId != null, Tag::getId, excludeId));
    }
}
