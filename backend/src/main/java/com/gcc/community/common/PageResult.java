package com.gcc.community.common;

import java.util.List;

// 分页返回格式。
public record PageResult<T>(List<T> items, long total, int pageNum, int pageSize) {
}
