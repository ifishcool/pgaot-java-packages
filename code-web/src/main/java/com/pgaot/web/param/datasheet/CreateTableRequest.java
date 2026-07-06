package com.pgaot.web.param.datasheet;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "建表请求")
public class CreateTableRequest {
    @Schema(description = "表名", example = "scores")
    private String name;
    @Schema(description = "表标题", example = "成绩表")
    private String title;
    @Schema(description = "表描述")
    private String description;
    @Schema(description = "列定义")
    private List<ColumnParam> columns;

    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public List<ColumnParam> getColumns() { return columns; }
    public void setColumns(List<ColumnParam> v) { columns = v; }
}
