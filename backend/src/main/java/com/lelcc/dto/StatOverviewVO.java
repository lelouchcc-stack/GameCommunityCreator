package com.lelcc.dto;

import lombok.Data;

import java.util.List;

/**
 * 数据统计总览。
 *
 * <p>两组数字刻意分开，避免"看起来是一个数其实是两种口径"：
 * <ul>
 *   <li><b>累计快照</b>（total*）：不受时间范围影响，反映社区当前规模；</li>
 *   <li><b>范围内新增</b>（range*）：由 days 决定，反映这段时间的活跃度。</li>
 * </ul>
 * 统计口径与用户端一致：帖子只算已发布，评论只算正常状态。</p>
 */
@Data
public class StatOverviewVO {

    /** 统计天数（趋势图覆盖最近这么多天） */
    private Integer days;

    /* -------- 累计快照 -------- */

    private Long totalMembers;

    /** 已发布帖子 */
    private Long totalPosts;

    /** 待审核帖子 */
    private Long pendingPosts;

    private Long totalComments;

    private Long totalBoards;

    private Long totalNotices;

    /** 待处理举报 */
    private Long pendingReports;

    /* -------- 范围内新增 -------- */

    private Long rangeNewMembers;

    private Long rangeNewPosts;

    private Long rangeNewComments;

    /* -------- 图表数据 -------- */

    /** 逐日趋势，日期连续（没有数据的天补 0），保证柱子不会"跳日期" */
    private List<TrendPoint> trend;

    private List<BoardPoint> topBoards;

    private List<VersionPoint> versions;

    public record TrendPoint(String date, long posts, long comments) {
    }

    public record BoardPoint(long boardId, String boardName, long posts) {
    }

    public record VersionPoint(String versionNo, long posts, boolean current) {
    }
}
