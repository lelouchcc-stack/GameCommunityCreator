package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.BusinessException;
import com.lelcc.dto.BannerSlideDTO;
import com.lelcc.dto.BannerSlideVO;
import com.lelcc.entity.BannerSlide;
import com.lelcc.mapper.BannerSlideMapper;
import com.lelcc.service.BannerSlideService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BannerSlideServiceImpl implements BannerSlideService {

    private static final int STATUS_VISIBLE = 1;
    private static final int STATUS_HIDDEN = 0;

    @Resource
    private BannerSlideMapper bannerSlideMapper;

    @Override
    public List<BannerSlideVO> listAll() {
        return toVoList(selectByStatus(null));
    }

    @Override
    public List<BannerSlideVO> listVisible() {
        return toVoList(selectByStatus(STATUS_VISIBLE));
    }

    @Override
    public void create(BannerSlideDTO dto) {
        BannerSlide slide = new BannerSlide();
        slide.setTitle(dto.title());
        slide.setSubtitle(dto.subtitle());
        slide.setImage(dto.image());
        slide.setLinkUrl(dto.linkUrl());
        // 不传排序时排到最后，新加的轮播图不会突然顶掉第一张
        slide.setSort(dto.sort() == null ? nextSort() : dto.sort());
        slide.setStatus(STATUS_VISIBLE);
        bannerSlideMapper.insert(slide);
    }

    @Override
    public void update(Long id, BannerSlideDTO dto) {
        BannerSlide exists = requireSlide(id);
        BannerSlide slide = new BannerSlide();
        slide.setId(exists.getId());
        slide.setTitle(dto.title());
        slide.setSubtitle(dto.subtitle());
        slide.setImage(dto.image());
        slide.setLinkUrl(dto.linkUrl());
        // sort 传空表示不动原顺序（改动顺序走「上移/下移」的整组重排）
        slide.setSort(dto.sort());
        bannerSlideMapper.updateById(slide);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_VISIBLE && status != STATUS_HIDDEN)) {
            throw new BusinessException(400, "状态只能为 1（展示）或 0（隐藏）");
        }
        BannerSlide slide = requireSlide(id);
        BannerSlide update = new BannerSlide();
        update.setId(slide.getId());
        update.setStatus(status);
        bannerSlideMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "排序列表不能为空");
        }
        // 整组重编号，避免残留重复 sort 让"上移/下移"失效
        for (int i = 0; i < ids.size(); i++) {
            BannerSlide slide = requireSlide(ids.get(i));
            BannerSlide update = new BannerSlide();
            update.setId(slide.getId());
            update.setSort(i + 1);
            bannerSlideMapper.updateById(update);
        }
    }

    @Override
    public void delete(Long id) {
        requireSlide(id);
        bannerSlideMapper.deleteById(id);
    }

    private BannerSlide requireSlide(Long id) {
        BannerSlide slide = bannerSlideMapper.selectById(id);
        if (slide == null) {
            throw new BusinessException(404, "轮播图不存在");
        }
        return slide;
    }

    /** 新增时的默认顺序：排在当前最后一张之后 */
    private int nextSort() {
        BannerSlide last = bannerSlideMapper.selectOne(Wrappers.<BannerSlide>lambdaQuery()
                .orderByDesc(BannerSlide::getSort)
                .orderByDesc(BannerSlide::getId)
                .last("LIMIT 1"));
        return last == null || last.getSort() == null ? 1 : last.getSort() + 1;
    }

    /** status 为 null 时取全部（后台列表），否则按状态过滤（用户端只取展示中的） */
    private List<BannerSlide> selectByStatus(Integer status) {
        return bannerSlideMapper.selectList(Wrappers.<BannerSlide>lambdaQuery()
                .eq(status != null, BannerSlide::getStatus, status)
                .orderByAsc(BannerSlide::getSort)
                .orderByAsc(BannerSlide::getId));
    }

    private List<BannerSlideVO> toVoList(List<BannerSlide> slides) {
        return slides.stream().map(BannerSlideServiceImpl::toVo).toList();
    }

    private static BannerSlideVO toVo(BannerSlide slide) {
        BannerSlideVO vo = new BannerSlideVO();
        vo.setId(slide.getId());
        vo.setTitle(slide.getTitle());
        vo.setSubtitle(slide.getSubtitle());
        vo.setImage(slide.getImage());
        vo.setLinkUrl(slide.getLinkUrl());
        vo.setSort(slide.getSort());
        vo.setStatus(slide.getStatus());
        vo.setCreateTime(slide.getCreateTime());
        return vo;
    }
}
