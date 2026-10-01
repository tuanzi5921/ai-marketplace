package com.company.ai.marketplace.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应结构。
 * <p>与前端 {@code PageResult<T>} 约定一致：{ list, total, page, size }。
 * 用 {@link #from(Page, Function)} 从 MyBatis-Plus Page 转换。
 */
@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private long page;
    private long size;

    public static <S, T> PageResult<T> from(Page<S> page, Function<S, T> mapper) {
        PageResult<T> result = new PageResult<>();
        result.setList(page.getRecords().stream().map(mapper).toList());
        result.setTotal(page.getTotal());
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    public static <S> PageResult<S> from(Page<S> page) {
        return from(page, Function.identity());
    }
}
