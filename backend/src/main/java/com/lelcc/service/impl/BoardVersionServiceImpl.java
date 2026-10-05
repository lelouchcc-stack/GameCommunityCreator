package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.BusinessException;
import com.lelcc.dto.BoardVersionDTO;
import com.lelcc.dto.BoardVersionVO;
import com.lelcc.entity.Board;
import com.lelcc.entity.BoardVersion;
import com.lelcc.mapper.BoardMapper;
import com.lelcc.mapper.BoardVersionMapper;
import com.lelcc.service.BoardVersionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoardVersionServiceImpl implements BoardVersionService {

    private static final int BOARD_OPEN = 1;
    private static final int STATUS_NORMAL = 1;
    private static final int STATUS_DISABLED = 0;

    @Resource
    private BoardVersionMapper boardVersionMapper;

    @Resource
    private BoardMapper boardMapper;

    @Override
    public List<BoardVersionVO> listByBoard(Long boardId) {
        Board board = requireBoard(boardId);
        Long currentId = board.getCurrentVersionId();
        return boardVersionMapper.selectList(Wrappers.<BoardVersion>lambdaQuery()
                        .eq(BoardVersion::getBoardId, boardId)
                        .orderByDesc(BoardVersion::getReleaseTime)
                        .orderByDesc(BoardVersion::getId))
                .stream()
                .map(version -> toVo(version, currentId))
                .toList();
    }

    @Override
    public void create(Long boardId, BoardVersionDTO dto) {
        requireBoardOpen(boardId);
        if (versionNoExists(boardId, dto.versionNo(), null)) {
            throw new BusinessException(400, "版本号已存在，请修改");
        }
        BoardVersion version = new BoardVersion();
        version.setBoardId(boardId);
        version.setVersionNo(dto.versionNo());
        version.setVersionName(dto.versionName());
        version.setReleaseTime(dto.releaseTime());
        version.setUpdateNote(dto.updateNote());
        version.setStatus(STATUS_NORMAL);
        boardVersionMapper.insert(version);
    }

    @Override
    public void update(Long boardId, Long id, BoardVersionDTO dto) {
        requireBoardOpen(boardId);
        BoardVersion exists = requireVersion(boardId, id);
        if (versionNoExists(boardId, dto.versionNo(), id)) {
            throw new BusinessException(400, "版本号已存在，请修改");
        }
        BoardVersion version = new BoardVersion();
        version.setId(exists.getId());
        version.setVersionNo(dto.versionNo());
        version.setVersionName(dto.versionName());
        version.setReleaseTime(dto.releaseTime());
        version.setUpdateNote(dto.updateNote());
        boardVersionMapper.updateById(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setCurrent(Long boardId, Long id) {
        Board board = requireBoardOpen(boardId);
        BoardVersion version = requireVersion(boardId, id);
        if (!Integer.valueOf(STATUS_NORMAL).equals(version.getStatus())) {
            throw new BusinessException(400, "停用的版本不能设为当前版本");
        }
        Board update = new Board();
        update.setId(board.getId());
        update.setCurrentVersionId(version.getId());
        boardMapper.updateById(update);
    }

    @Override
    public void changeStatus(Long boardId, Long id, Integer status) {
        if (status == null || (status != STATUS_NORMAL && status != STATUS_DISABLED)) {
            throw new BusinessException(400, "状态只能为 1（正常）或 0（停用）");
        }
        requireBoardOpen(boardId);
        BoardVersion version = requireVersion(boardId, id);
        if (status == STATUS_DISABLED && version.getId().equals(currentVersionId(boardId))) {
            throw new BusinessException(400, "当前版本不能停用，请先切换当前版本");
        }
        BoardVersion update = new BoardVersion();
        update.setId(version.getId());
        update.setStatus(status);
        boardVersionMapper.updateById(update);
    }

    @Override
    public void delete(Long boardId, Long id) {
        requireBoardOpen(boardId);
        BoardVersion version = requireVersion(boardId, id);
        if (version.getId().equals(currentVersionId(boardId))) {
            throw new BusinessException(400, "当前版本不能删除，请先切换当前版本");
        }
        long postCount = boardVersionMapper.countPostsByVersionId(id);
        if (postCount > 0) {
            throw new BusinessException(400,
                    "该版本已被 " + postCount + " 篇帖子引用，无法删除，请改为停用");
        }
        boardVersionMapper.deleteById(id);
    }

    private Board requireBoard(Long boardId) {
        Board board = boardId == null ? null : boardMapper.selectById(boardId);
        if (board == null) {
            throw new BusinessException(404, "板块不存在");
        }
        return board;
    }

    /** 内容维度配置要求板块处于开放状态 */
    private Board requireBoardOpen(Long boardId) {
        Board board = requireBoard(boardId);
        if (!Integer.valueOf(BOARD_OPEN).equals(board.getStatus())) {
            throw new BusinessException(400, "板块已关闭，禁止配置内容维度");
        }
        return board;
    }

    private Long currentVersionId(Long boardId) {
        Board board = boardMapper.selectById(boardId);
        return board == null ? null : board.getCurrentVersionId();
    }

    private BoardVersion requireVersion(Long boardId, Long id) {
        BoardVersion version = boardVersionMapper.selectById(id);
        if (version == null || !version.getBoardId().equals(boardId)) {
            throw new BusinessException(404, "版本不存在");
        }
        return version;
    }

    private boolean versionNoExists(Long boardId, String versionNo, Long excludeId) {
        return boardVersionMapper.exists(Wrappers.<BoardVersion>lambdaQuery()
                .eq(BoardVersion::getBoardId, boardId)
                .eq(BoardVersion::getVersionNo, versionNo)
                .ne(excludeId != null, BoardVersion::getId, excludeId));
    }

    private BoardVersionVO toVo(BoardVersion version, Long currentId) {
        BoardVersionVO vo = new BoardVersionVO();
        vo.setId(version.getId());
        vo.setBoardId(version.getBoardId());
        vo.setVersionNo(version.getVersionNo());
        vo.setVersionName(version.getVersionName());
        vo.setReleaseTime(version.getReleaseTime());
        vo.setUpdateNote(version.getUpdateNote());
        vo.setStatus(version.getStatus());
        vo.setIsCurrent(version.getId().equals(currentId));
        vo.setCreateTime(version.getCreateTime());
        return vo;
    }
}
