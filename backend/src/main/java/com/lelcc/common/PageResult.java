package com.lelcc.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 统一分页返回结构。不直接暴露 MyBatis-Plus 的 Page，
 * 前端只需要 list + total，也不用关心分页实现。
 */
public record PageResult<T>(List<T> list, long total) {

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal());
    }
}
