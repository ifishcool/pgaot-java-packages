package com.pgaot.sql.support;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 通用分页响应 */
public class PageResponse<T> {

    private List<T> rows;
    private long total;
    private int page;
    private int size;
    private int pages;

    public List<T> getRows() { return rows; }
    public void setRows(List<T> rows) { this.rows = rows; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getPages() { return pages; }
    public void setPages(int pages) { this.pages = pages; }

    public static <T> PageResponse<T> of(List<T> rows, long total, int page, int size) {
        PageResponse<T> r = new PageResponse<>();
        r.setRows(rows);
        r.setTotal(total);
        r.setPage(page);
        r.setSize(size);
        r.setPages((int) Math.ceil((double) total / size));
        return r;
    }

    public static <T> PageResponse<T> empty(int page, int size) {
        PageResponse<T> r = new PageResponse<>();
        r.setRows(Collections.emptyList());
        r.setTotal(0);
        r.setPage(page);
        r.setSize(size);
        r.setPages(0);
        return r;
    }

    /** 类型转换 */
    public <U> PageResponse<U> convert(Function<T, U> mapper) {
        PageResponse<U> r = new PageResponse<>();
        r.setRows(rows.stream().map(mapper).collect(Collectors.toList()));
        r.setTotal(total);
        r.setPage(page);
        r.setSize(size);
        r.setPages(pages);
        return r;
    }
}
