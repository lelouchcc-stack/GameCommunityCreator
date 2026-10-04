package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lelcc.common.BusinessException;
import com.lelcc.common.PageResult;
import com.lelcc.dto.BoardDTO;
import com.lelcc.dto.BoardVO;
import com.lelcc.entity.Board;
import com.lelcc.mapper.BoardMapper;
import com.lelcc.mapper.PostMapper;
import com.lelcc.service.BoardService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardServiceImpl implements BoardService {

    private static final int STATUS_OPEN = 1;
    private static final int STATUS_CLOSED = 0;
    private static final int DEFAULT_SORT = 0;

    @Resource
    private BoardMapper boardMapper;

    @Resource
    private PostMapper postMapper;

    @Override
    public PageResult<BoardVO> page(String name, Integer status, long pageNum, long pageSize) {
        Page<BoardVO> page = new Page<>(pageNum, pageSize);
        return PageResult.of(boardMapper.selectBoardPage(page, name, status));
    }

    @Override
    public List<BoardVO> listOpen() {
        return boardMapper.selectOpenList();
    }

    @Override
    public void create(BoardDTO dto) {
        if (nameExists(dto.name(), null)) {
            throw new BusinessException(400, "板块名称已存在，请修改");
        }
        Board board = new Board();
        board.setName(dto.name());
        board.setIntro(dto.intro());
        board.setCover(dto.cover());
        board.setSort(dto.sort() == null ? DEFAULT_SORT : dto.sort());
        board.setStatus(STATUS_OPEN);
        boardMapper.insert(board);
    }

    @Override
    public void update(Long id, BoardDTO dto) {
        Board exists = requireBoard(id);
        if (nameExists(dto.name(), id)) {
            throw new BusinessException(400, "板块名称已存在，请修改");
        }
        Board board = new Board();
        board.setId(exists.getId());
        board.setName(dto.name());
        board.setIntro(dto.intro());
        board.setCover(dto.cover());
        board.setSort(dto.sort() == null ? DEFAULT_SORT : dto.sort());
        boardMapper.updateById(board);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_OPEN && status != STATUS_CLOSED)) {
            throw new BusinessException(400, "状态只能为 1（开放）或 0（关闭）");
        }
        Board board = requireBoard(id);
        Board update = new Board();
        update.setId(board.getId());
        update.setStatus(status);
        boardMapper.updateById(update);
    }

    @Override
    public void delete(Long id) {
        Board board = requireBoard(id);
        // 物理外键没有，关联校验必须在应用层做：板块下有帖子就不许删，只能关闭
        long postCount = postMapper.countByBoardId(board.getId());
        if (postCount > 0) {
            throw new BusinessException(400,
                    "该板块下还有 " + postCount + " 篇帖子，无法删除，请改为关闭板块");
        }
        boardMapper.deleteById(id);
    }

    private Board requireBoard(Long id) {
        Board board = boardMapper.selectById(id);
        if (board == null) {
            throw new BusinessException(404, "板块不存在");
        }
        return board;
    }

    /** 名称唯一校验，excludeId 用于编辑时排除自己 */
    private boolean nameExists(String name, Long excludeId) {
        return boardMapper.exists(Wrappers.<Board>lambdaQuery()
                .eq(Board::getName, name)
                .ne(excludeId != null, Board::getId, excludeId));
    }
}
