package com.lelcc.service;

import com.lelcc.common.PageResult;
import com.lelcc.dto.AppPostDetailVO;
import com.lelcc.dto.AppPostItemVO;
import com.lelcc.dto.HotPostVO;
import com.lelcc.dto.PostCreateDTO;
import com.lelcc.dto.PostCreateVO;

import java.util.List;

/** 帖子读取与发布：首页热门榜 + 用户端列表检索与详情 + 玩家发帖。 */
public interface PostService {

    /** 热门帖子（置顶优先，再按点赞、浏览降序），最多 limit 条 */
    List<HotPostVO> listHot(int limit);

    /**
     * 用户端帖子检索：只返回已发布且板块开放的帖子，分页；
     * 版本与板块当前版本不一致的帖子标记 expired。
     */
    PageResult<AppPostItemVO> pagePublished(Long boardId, Long versionId, Long stageId, Long tagId,
                                            String keyword, String sort, long pageNum, long pageSize);

    /** 帖子详情：累加浏览量；帖子未发布或不存在时抛业务异常 */
    AppPostDetailVO detail(Long postId);

    /**
     * 「我的帖子」：当前登录作者的帖子，含待审核与已下架的（作者自己要看得见）。
     * 只读自己的，不接受外部传入 memberId。
     */
    PageResult<AppPostItemVO> pageMine(long pageNum, long pageSize);

    /**
     * 玩家发帖：校验板块开放、版本/阶段属于该板块、标签可用，落库为「待审核」。
     * 审核通过前不出现在任何公开列表里，只在该作者的「我的帖子」中可见。
     */
    PostCreateVO create(PostCreateDTO dto);
}
