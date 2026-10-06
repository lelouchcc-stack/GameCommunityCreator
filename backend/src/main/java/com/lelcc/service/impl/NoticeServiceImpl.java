package com.lelcc.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lelcc.common.BusinessException;
import com.lelcc.common.PageResult;
import com.lelcc.dto.NoticeDTO;
import com.lelcc.dto.NoticeVO;
import com.lelcc.entity.Notice;
import com.lelcc.mapper.NoticeMapper;
import com.lelcc.service.NoticeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {

    /** 首页公告栏放不下太多条，上限兜一下，避免调用方传个巨大的 limit 把整表拉出来 */
    private static final int MAX_LIMIT = 20;
    private static final int DEFAULT_LIMIT = 5;
    private static final long MAX_PAGE_SIZE = 50;

    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_DRAFT = 0;
    private static final int DEFAULT_SORT = 0;

    @Resource
    private NoticeMapper noticeMapper;

    @Override
    public List<NoticeVO> listPublished(int limit) {
        return noticeMapper.selectPublished(normalize(limit));
    }

    @Override
    public PageResult<NoticeVO> pagePublished(long pageNum, long pageSize) {
        Page<NoticeVO> page = new Page<>(pageNum < 1 ? 1 : pageNum,
                pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE));
        return PageResult.of(noticeMapper.selectPublishedPage(page));
    }

    @Override
    public NoticeVO detail(Long id) {
        NoticeVO notice = noticeMapper.selectPublishedById(id);
        if (notice == null) {
            throw new BusinessException(404, "公告不存在或未发布");
        }
        return notice;
    }

    @Override
    public PageResult<NoticeVO> page(Integer status, Integer type, String keyword, long pageNum, long pageSize) {
        requireKnownStatus(status);
        Page<NoticeVO> page = new Page<>(pageNum < 1 ? 1 : pageNum,
                pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE));
        return PageResult.of(noticeMapper.selectAdminPage(page, status, type, keyword));
    }

    @Override
    public NoticeVO adminDetail(Long id) {
        // 后台要能看草稿，所以这里不复用 detail()（那个只认已发布的）
        Notice notice = requireNotice(id);
        NoticeVO vo = new NoticeVO();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setContent(notice.getContent());
        vo.setType(notice.getType());
        vo.setSort(notice.getSort());
        vo.setStatus(notice.getStatus());
        vo.setCreateTime(notice.getCreateTime());
        vo.setUpdateTime(notice.getUpdateTime());
        return vo;
    }

    @Override
    public void create(NoticeDTO dto) {
        Notice notice = new Notice();
        notice.setTitle(dto.title());
        notice.setContent(dto.content());
        notice.setType(dto.type());
        notice.setSort(dto.sort() == null ? DEFAULT_SORT : dto.sort());
        // 不传状态按"直接发布"处理：管理员点新增时多数就是想发出去
        notice.setStatus(dto.status() == null ? STATUS_PUBLISHED : dto.status());
        noticeMapper.insert(notice);
    }

    @Override
    public void update(Long id, NoticeDTO dto) {
        Notice exists = requireNotice(id);
        Notice update = new Notice();
        update.setId(exists.getId());
        update.setTitle(dto.title());
        update.setContent(dto.content());
        update.setType(dto.type());
        update.setSort(dto.sort() == null ? DEFAULT_SORT : dto.sort());
        if (dto.status() != null) {
            update.setStatus(dto.status());
        }
        noticeMapper.updateById(update);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        requireKnownStatus(status);
        requireNotice(id);
        Notice update = new Notice();
        update.setId(id);
        update.setStatus(status);
        noticeMapper.updateById(update);
    }

    @Override
    public void delete(Long id) {
        requireNotice(id);
        noticeMapper.deleteById(id);
    }

    private Notice requireNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(404, "公告不存在");
        }
        return notice;
    }

    private void requireKnownStatus(Integer status) {
        if (status != null && status != STATUS_PUBLISHED && status != STATUS_DRAFT) {
            throw new BusinessException(400, "状态只能为 1（发布）或 0（草稿）");
        }
    }

    private int normalize(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
