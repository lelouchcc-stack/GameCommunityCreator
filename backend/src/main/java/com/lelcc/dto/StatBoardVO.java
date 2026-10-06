package com.lelcc.dto;

import lombok.Data;

/** 热门板块一行：该板块的已发布帖子数 */
@Data
public class StatBoardVO {

    private Long boardId;

    private String boardName;

    private Long total;
}
