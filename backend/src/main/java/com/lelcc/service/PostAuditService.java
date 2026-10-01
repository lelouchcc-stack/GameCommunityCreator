package com.lelcc.service;

import com.lelcc.common.PageResult;
import com.lelcc.dto.PostAuditVO;

public interface PostAuditService {

    PageResult<PostAuditVO> page(Integer status, Long boardId, String keyword, long pageNum, long pageSize);

    /** 审核帖子：通过(1)或下架(2)。下架必须填原因。 */
    void audit(Long id, Integer status, String reason);
}
