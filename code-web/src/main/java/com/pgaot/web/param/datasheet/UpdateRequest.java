package com.pgaot.web.param.datasheet;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新行请求")
public class UpdateRequest {
    @Schema(description = "表 ID")
    private String tableId;
    @Schema(description = "WHERE 条件", example = "name = '张三'")
    private String where;
    @Schema(description = "列名", example = "score")
    private String column;
    @Schema(description = "新值", example = "100")
    private String value;

    public String getTableId() { return tableId; }
    public void setTableId(String v) { tableId = v; }
    public String getWhere() { return where; }
    public void setWhere(String v) { where = v; }
    public String getColumn() { return column; }
    public void setColumn(String v) { column = v; }
    public String getValue() { return value; }
    public void setValue(String v) { value = v; }
}
