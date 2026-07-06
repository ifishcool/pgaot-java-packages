package com.pgaot.web.param.datasheet;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "列定义")
public class ColumnParam {
    @Schema(description = "列名", example = "score")
    private String name;
    @Schema(description = "列类型: STRING/TEXT/INT/DECIMAL 等", example = "DECIMAL")
    private String type;
    @Schema(description = "是否必填", example = "false")
    private boolean required;

    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getType() { return type; }
    public void setType(String v) { type = v; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean v) { required = v; }
}
