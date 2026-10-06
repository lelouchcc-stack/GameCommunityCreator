package com.lelcc.service.impl;

import com.lelcc.dto.StatBoardVO;
import com.lelcc.dto.StatOverviewVO;
import com.lelcc.dto.StatTrendVO;
import com.lelcc.dto.StatVersionVO;
import com.lelcc.mapper.StatMapper;
import com.lelcc.service.StatService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatServiceImpl implements StatService {

    /** 帖子状态：0待审 1已发布 2已下架 */
    private static final int POST_PUBLISHED = 1;
    private static final int POST_PENDING = 0;

    private static final int DEFAULT_DAYS = 30;
    private static final int MIN_DAYS = 7;
    private static final int MAX_DAYS = 90;
    /** 热门板块取前几名 */
    private static final int TOP_BOARD_LIMIT = 8;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private StatMapper statMapper;

    @Override
    public StatOverviewVO overview(Integer days) {
        int range = normalize(days);
        // 含今天往前数 range 天
        LocalDate from = LocalDate.now().minusDays(range - 1L);

        StatOverviewVO vo = new StatOverviewVO();
        vo.setDays(range);

        vo.setTotalMembers(statMapper.countMembers(null));
        vo.setTotalPosts(statMapper.countPosts(POST_PUBLISHED, null));
        vo.setPendingPosts(statMapper.countPosts(POST_PENDING, null));
        vo.setTotalComments(statMapper.countComments(null));
        vo.setTotalBoards(statMapper.countOpenBoards());
        vo.setTotalNotices(statMapper.countPublishedNotices());
        vo.setPendingReports(statMapper.countPendingReports());

        vo.setRangeNewMembers(statMapper.countMembers(from));
        vo.setRangeNewPosts(statMapper.countPosts(POST_PUBLISHED, from));
        vo.setRangeNewComments(statMapper.countComments(from));

        vo.setTrend(buildTrend(from, range));
        vo.setTopBoards(toBoardPoints(statMapper.selectTopBoards(TOP_BOARD_LIMIT)));
        vo.setVersions(toVersionPoints(statMapper.selectVersionDistribution()));
        return vo;
    }

    /**
     * 拼出连续日期序列：SQL 只返回有数据的天，这里补 0。
     * 不补的话柱状图会把 10-01 和 10-05 并排画在一起，看趋势就是错的。
     */
    private List<StatOverviewVO.TrendPoint> buildTrend(LocalDate from, int range) {
        Map<String, Long> posts = toDayMap(statMapper.selectDailyPosts(from));
        Map<String, Long> comments = toDayMap(statMapper.selectDailyComments(from));
        List<StatOverviewVO.TrendPoint> points = new ArrayList<>(range);
        for (int i = 0; i < range; i++) {
            String day = from.plusDays(i).format(DAY);
            points.add(new StatOverviewVO.TrendPoint(day,
                    posts.getOrDefault(day, 0L), comments.getOrDefault(day, 0L)));
        }
        return points;
    }

    private Map<String, Long> toDayMap(List<StatTrendVO> rows) {
        Map<String, Long> map = new HashMap<>();
        for (StatTrendVO row : rows) {
            if (row.getDate() != null) {
                map.put(row.getDate(), row.getTotal() == null ? 0L : row.getTotal());
            }
        }
        return map;
    }

    private List<StatOverviewVO.BoardPoint> toBoardPoints(List<StatBoardVO> rows) {
        List<StatOverviewVO.BoardPoint> points = new ArrayList<>(rows.size());
        for (StatBoardVO row : rows) {
            points.add(new StatOverviewVO.BoardPoint(
                    row.getBoardId() == null ? 0L : row.getBoardId(),
                    row.getBoardName(),
                    row.getTotal() == null ? 0L : row.getTotal()));
        }
        return points;
    }

    private List<StatOverviewVO.VersionPoint> toVersionPoints(List<StatVersionVO> rows) {
        List<StatOverviewVO.VersionPoint> points = new ArrayList<>(rows.size());
        for (StatVersionVO row : rows) {
            points.add(new StatOverviewVO.VersionPoint(
                    row.getVersionNo(),
                    row.getTotal() == null ? 0L : row.getTotal(),
                    row.getIsCurrent() != null && row.getIsCurrent() == 1));
        }
        return points;
    }

    private int normalize(Integer days) {
        if (days == null) {
            return DEFAULT_DAYS;
        }
        return Math.max(MIN_DAYS, Math.min(days, MAX_DAYS));
    }
}
