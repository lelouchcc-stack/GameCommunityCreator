package com.lelcc.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户端板块详情：板块本体 + 游戏版本 + 进度阶段。
 * 字段名与逻辑原子 data.boardDetail 的端口（board / listBoardVersion / listProgressStage）一致，
 * 这样图里的按名取端口才能命中。
 */
@Data
public class AppBoardDetailVO {

    private BoardVO board;

    private List<BoardVersionVO> listBoardVersion;

    private List<ProgressStageVO> listProgressStage;
}
