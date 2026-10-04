package com.lelcc.service;

import com.lelcc.dto.BannerSlideDTO;
import com.lelcc.dto.BannerSlideVO;

import java.util.List;

/** 首页轮播图：后台维护整组，用户端匿名读展示中的那几张。 */
public interface BannerSlideService {

    /** 后台列表：含隐藏项，按展示顺序 */
    List<BannerSlideVO> listAll();

    /** 用户端轮播：只取展示中的，按展示顺序 */
    List<BannerSlideVO> listVisible();

    void create(BannerSlideDTO dto);

    void update(Long id, BannerSlideDTO dto);

    /** 设为展示(1)或隐藏(0)，不做物理删除 */
    void changeStatus(Long id, Integer status);

    /** 按传入的 id 顺序整体重排序（后台「上移/下移」，sort 重编为 1..n） */
    void reorder(List<Long> ids);

    void delete(Long id);
}
