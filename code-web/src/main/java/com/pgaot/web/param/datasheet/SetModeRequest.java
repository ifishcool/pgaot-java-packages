package com.pgaot.web.param.datasheet;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "设置表模式")
public class SetModeRequest {
    @Schema(description = "模式: READ_ONLY / WRITE_ONLY / READ_WRITE / ALL", example = "READ_ONLY")
    private String mode;
    public String getMode() { return mode; }
    public void setMode(String v) { mode = v; }
}
