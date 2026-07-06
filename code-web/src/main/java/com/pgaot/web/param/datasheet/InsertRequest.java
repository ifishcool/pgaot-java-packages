package com.pgaot.web.param.datasheet;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "插入行请求")
public class InsertRequest {
    @Schema(description = "表 ID")
    private String tableId;
    @Schema(description = "行数据列表")
    private List<Map<String, Object>> rows;

    public String getTableId() { return tableId; }
    public void setTableId(String v) { tableId = v; }
    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> v) { rows = v; }
}
