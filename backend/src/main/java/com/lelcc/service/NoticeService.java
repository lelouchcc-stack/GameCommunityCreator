package com.lelcc.service;

import com.lelcc.common.PageResult;
import com.lelcc.dto.NoticeDTO;
import com.lelcc.dto.NoticeVO;

import java.util.List;

/**
 * 公告：用户端只读（首页公告栏、公告页）+ 后台维护（公告管理）。
 * 用户端那三个方法只暴露已发布公告，草稿永不下发。
 */
public interface NoticeService {

    /** 已发布的公告，按展示顺序，最多 limit 条（limit 由调用方给出，内部会收敛范围） */
    List<NoticeVO> listPublished(int limit);

    /** 已发布公告分页：公告列表页用 */
    PageResult<NoticeVO> pagePublished(long pageNum, long pageSize);

    /** 公告详情：不存在或未发布时抛业务异常 */
    NoticeVO detail(Long id);

    /* -------- 以下为后台维护 -------- */

    /** 后台分页：含草稿，可按状态/类型/关键字筛选 */
    PageResult<NoticeVO> page(Integer status, Integer type, String keyword, long pageNum, long pageSize);

    /** 后台详情：草稿也要能读（编辑弹窗回填用），与用户端的 detail 区别就在这儿 */
    NoticeVO adminDetail(Long id);

    void create(NoticeDTO dto);

    void update(Long id, NoticeDTO dto);

    /** 发布(1) / 转草稿(0) */
    void changeStatus(Long id, Integer status);

    void delete(Long id);
}
