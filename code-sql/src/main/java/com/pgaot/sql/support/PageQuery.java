package com.pgaot.sql.support;

import com.pgaot.sql.common.constants.SqlConstants;
import com.pgaot.sql.common.constants.Messages;
import com.pgaot.sql.exception.SqlException;

/**
 * 分页查询参数.
 *
 * <pre>{@code
 * PageQuery pq = new PageQuery(1, 10);
 * PageResponse<Map<String, Object>> page = db.page("SELECT * FROM t_user WHERE age > ?", pq, 18);
 * }</pre>
 */
public class PageQuery {

    private final int page;
    private final int size;

    /** @param page 页码，从 1 开始 */
    public PageQuery(int page, int size) {
        if (page < SqlConstants.Page.MIN_PAGE)
            throw SqlException.pageParamInvalid(Messages.PAGE_MIN + SqlConstants.Page.MIN_PAGE
                    + Messages.PAGE_CURRENT + page);
        if (size < SqlConstants.Page.MIN_SIZE || size > SqlConstants.Page.MAX_SIZE)
            throw SqlException.pageParamInvalid(Messages.PAGE_RANGE + SqlConstants.Page.MIN_SIZE
                    + "~" + SqlConstants.Page.MAX_SIZE + Messages.PAGE_CURRENT + size);
        this.page = page;
        this.size = size;
    }

    public int getPage() { return page; }
    public int getSize() { return size; }
    /** LIMIT 偏移量 */
    public int getOffset() { return (page - 1) * size; }
}
