package com.zwei.common.core.page;

import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 统一分页响应格式。
 * <p>
 * 替代 Controller 中手写 HashMap 的分页组装，提供类型安全的分页结果封装。
 */
public class PageResult<T> {
    private List<T> rows;
    private long total;
    private int pageNum;
    private int pageSize;

    public PageResult() {
    }

    /**
     * 从列表和分页参数构建 PageResult。
     */
    public static <T> PageResult<T> of(List<T> rows, PageDomain pageDomain) {
        PageResult<T> result = new PageResult<>();
        result.rows = rows;
        result.total = new PageInfo<>(rows).getTotal();
        result.pageNum = pageDomain.getPageNum();
        result.pageSize = pageDomain.getPageSize();
        return result;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
