package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.BusinessException;
import com.lelcc.dto.ProgressStageDTO;
import com.lelcc.dto.ProgressStageVO;
import com.lelcc.dto.StageSortDTO;
import com.lelcc.entity.Board;
import com.lelcc.entity.ProgressStage;
import com.lelcc.mapper.BoardMapper;
import com.lelcc.mapper.ProgressStageMapper;
import com.lelcc.service.ProgressStageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProgressStageServiceImpl implements ProgressStageService {

    private static final int BOARD_OPEN = 1;
    private static final int STATUS_NORMAL = 1;
    private static final int STATUS_DISABLED = 0;
    private static final int DEFAULT_SORT = 0;

    @Resource
    private ProgressStageMapper progressStageMapper;

    @Resource
    private BoardMapper boardMapper;

    @Override
    public List<ProgressStageVO> listByBoard(Long boardId) {
        requireBoard(boardId);
        return progressStageMapper.selectList(Wrappers.<ProgressStage>lambdaQuery()
                        .eq(ProgressStage::getBoardId, boardId)
                        .orderByAsc(ProgressStage::getSort)
                        .orderByAsc(ProgressStage::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public void create(Long boardId, ProgressStageDTO dto) {
        requireBoardOpen(boardId);
        ProgressStage stage = new ProgressStage();
        stage.setBoardId(boardId);
        stage.setStageName(dto.stageName());
        stage.setDescription(dto.description());
        stage.setSort(dto.sort() == null ? DEFAULT_SORT : dto.sort());
        stage.setStatus(STATUS_NORMAL);
        progressStageMapper.insert(stage);
    }

    @Override
    public void update(Long boardId, Long id, ProgressStageDTO dto) {
        requireBoardOpen(boardId);
        ProgressStage exists = requireStage(boardId, id);
        ProgressStage stage = new ProgressStage();
        stage.setId(exists.getId());
        stage.setStageName(dto.stageName());
        stage.setDescription(dto.description());
        stage.setSort(dto.sort() == null ? DEFAULT_SORT : dto.sort());
        progressStageMapper.updateById(stage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorder(Long boardId, List<StageSortDTO> items) {
        requireBoardOpen(boardId);
        if (items == null || items.isEmpty()) {
            return;
        }
        for (StageSortDTO item : items) {
            ProgressStage exists = requireStage(boardId, item.id());
            ProgressStage update = new ProgressStage();
            update.setId(exists.getId());
            update.setSort(item.sort());
            progressStageMapper.updateById(update);
        }
    }

    @Override
    public void changeStatus(Long boardId, Long id, Integer status) {
        if (status == null || (status != STATUS_NORMAL && status != STATUS_DISABLED)) {
            throw new BusinessException(400, "状态只能为 1（正常）或 0（停用）");
        }
        requireBoardOpen(boardId);
        ProgressStage exists = requireStage(boardId, id);
        ProgressStage update = new ProgressStage();
        update.setId(exists.getId());
        update.setStatus(status);
        progressStageMapper.updateById(update);
    }

    @Override
    public void delete(Long boardId, Long id) {
        requireBoardOpen(boardId);
        ProgressStage exists = requireStage(boardId, id);
        long postCount = progressStageMapper.countPostsByStageId(id);
        if (postCount > 0) {
            throw new BusinessException(400,
                    "该阶段已被 " + postCount + " 篇帖子引用，无法删除，请改为停用");
        }
        long progressCount = progressStageMapper.countMemberProgressByStageId(id);
        if (progressCount > 0) {
            throw new BusinessException(400,
                    "该阶段已被 " + progressCount + " 名玩家设为游戏进度，无法删除，请改为停用");
        }
        progressStageMapper.deleteById(exists.getId());
    }

    private Board requireBoard(Long boardId) {
        Board board = boardId == null ? null : boardMapper.selectById(boardId);
        if (board == null) {
            throw new BusinessException(404, "板块不存在");
        }
        return board;
    }

    private Board requireBoardOpen(Long boardId) {
        Board board = requireBoard(boardId);
        if (!Integer.valueOf(BOARD_OPEN).equals(board.getStatus())) {
            throw new BusinessException(400, "板块已关闭，禁止配置内容维度");
        }
        return board;
    }

    private ProgressStage requireStage(Long boardId, Long id) {
        ProgressStage stage = progressStageMapper.selectById(id);
        if (stage == null || !stage.getBoardId().equals(boardId)) {
            throw new BusinessException(404, "进度阶段不存在");
        }
        return stage;
    }

    private ProgressStageVO toVo(ProgressStage stage) {
        ProgressStageVO vo = new ProgressStageVO();
        vo.setId(stage.getId());
        vo.setBoardId(stage.getBoardId());
        vo.setStageName(stage.getStageName());
        vo.setDescription(stage.getDescription());
        vo.setSort(stage.getSort());
        vo.setStatus(stage.getStatus());
        vo.setCreateTime(stage.getCreateTime());
        return vo;
    }
}
