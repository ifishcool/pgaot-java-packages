package com.pgaot.web.param.datasheet;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "删除行请求")
public class DeleteRequest {
    @Schema(description = "表 ID")
    private String tableId;
    @Schema(description = "WHERE 条件", example = "name = 'test'")
    private String where;

    public String getTableId() { return tableId; }
    public void setTableId(String v) { tableId = v; }
    public String getWhere() { return where; }
    public void setWhere(String v) { where = v; }
}
