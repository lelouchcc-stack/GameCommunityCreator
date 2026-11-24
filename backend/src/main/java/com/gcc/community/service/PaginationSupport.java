package com.gcc.community.service;

import com.gcc.community.common.ApiException;

/** 分页参数的小工具。 */
final class PaginationSupport {

    static final int DEFAULT_PAGE_SIZE = 10;
    static final int MAX_PAGE_SIZE = 100;

    private PaginationSupport() {
    }

    static int requirePageNum(int pageNum) {
        if (pageNum < 1) {
            throw ApiException.badRequest("pageNum 必须大于等于 1");
        }
        return pageNum;
    }

    static int requirePageSize(int pageSize) {
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("pageSize 必须在 1 到 100 之间");
        }
        return pageSize;
    }

    static int offset(int pageNum, int pageSize) {
        return (pageNum - 1) * pageSize;
    }
}
