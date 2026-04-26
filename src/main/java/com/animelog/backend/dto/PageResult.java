package com.animelog.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 封装分页查询结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    private int pageNum; // 当前页码（从 1 开始）

    private int pageSize; // 每页大小

    private long total; //总记录数

    private long pages; // 总页数

    private boolean hasNext; // 是否有下一页

    private List<T> records; //当前页数据集合

    /**
     * 构建标准分页对象。
     *
     * @param pageNum 当前页
     * @param pageSize 每页大小
     * @param total 总记录数
     * @param records 当前页记录
     * @param <T> 记录类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(int pageNum, int pageSize, long total, List<T> records) {
        long pages = pageSize <= 0 ? 0 : (long) Math.ceil((double) total / pageSize);
        boolean hasNext = pageNum < pages;
        return PageResult.<T>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(total)
                .pages(pages)
                .hasNext(hasNext)
                .records(records)
                .build();
    }

}
