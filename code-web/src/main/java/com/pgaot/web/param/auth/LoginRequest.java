package com.pgaot.web.param.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录请求")
public class LoginRequest {
    @Schema(description = "登录方式", example = "YUNTOWER")
    private String type;
    @Schema(description = "授权码")
    private String code;

    public String getType() { return type; }
    public void setType(String v) { type = v; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
}
