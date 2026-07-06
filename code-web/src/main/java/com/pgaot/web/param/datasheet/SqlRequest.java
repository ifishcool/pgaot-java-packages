package com.pgaot.web.param.datasheet;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SQL 执行请求")
public class SqlRequest {
    @Schema(description = "SQL 语句", example = "SELECT * FROM scores")
    private String sql;
    public String getSql() { return sql; }
    public void setSql(String v) { sql = v; }
}
