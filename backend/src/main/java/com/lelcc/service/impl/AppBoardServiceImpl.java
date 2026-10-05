package com.lelcc.service.impl;

import com.lelcc.common.BusinessException;
import com.lelcc.dto.AppBoardDetailVO;
import com.lelcc.dto.BoardVO;
import com.lelcc.entity.Board;
import com.lelcc.entity.BoardVersion;
import com.lelcc.mapper.BoardMapper;
import com.lelcc.mapper.BoardVersionMapper;
import com.lelcc.service.AppBoardService;
import com.lelcc.service.BoardVersionService;
import com.lelcc.service.ProgressStageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AppBoardServiceImpl implements AppBoardService {

    private static final int BOARD_OPEN = 1;
    private static final int STATUS_NORMAL = 1;

    @Resource
    private BoardMapper boardMapper;

    @Resource
    private BoardVersionMapper boardVersionMapper;

    @Resource
    private BoardVersionService boardVersionService;

    @Resource
    private ProgressStageService progressStageService;

    @Override
    public AppBoardDetailVO detail(Long boardId) {
        Board board = boardId == null ? null : boardMapper.selectById(boardId);
        if (board == null || !Integer.valueOf(BOARD_OPEN).equals(board.getStatus())) {
            throw new BusinessException(404, "该板块暂未开放");
        }
        AppBoardDetailVO vo = new AppBoardDetailVO();
        vo.setBoard(toBoardVo(board));
        // 用户端只展示启用中的版本与阶段
        vo.setListBoardVersion(boardVersionService.listByBoard(boardId).stream()
                .filter(version -> Integer.valueOf(STATUS_NORMAL).equals(version.getStatus()))
                .toList());
        vo.setListProgressStage(progressStageService.listByBoard(boardId).stream()
                .filter(stage -> Integer.valueOf(STATUS_NORMAL).equals(stage.getStatus()))
                .toList());
        return vo;
    }

    private BoardVO toBoardVo(Board board) {
        BoardVO vo = new BoardVO();
        vo.setId(board.getId());
        vo.setName(board.getName());
        vo.setIntro(board.getIntro());
        vo.setCover(board.getCover());
        vo.setSort(board.getSort());
        vo.setStatus(board.getStatus());
        vo.setCurrentVersionId(board.getCurrentVersionId());
        if (board.getCurrentVersionId() != null) {
            BoardVersion current = boardVersionMapper.selectById(board.getCurrentVersionId());
            if (current != null) {
                vo.setCurrentVersionNo(current.getVersionNo());
            }
        }
        vo.setCreateTime(board.getCreateTime());
        return vo;
    }
}
