package com.pgaot.web.common;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.pgaot.log.api.LogContext;

/**
 * 统一 API 响应体.
 *
 * <p>成功: {@code {"code":200, "data":..., "traceId":"xxx"}}
 * <p>失败: {@code {"code":401, "message":"未登录", "traceId":"xxx"}}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int code;
    private final String message;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final T data;
    private final String traceId;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = LogContext.isInitialized() ? LogContext.getTraceId() : null;
    }

    /** 成功（有数据） */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, null, data);
    }

    /** 成功（无数据） — data 序列化为 {} */
    @SuppressWarnings("unchecked")
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(200, null, (T) Map.of());
    }

    /** 失败 */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public String getTraceId() { return traceId; }
}
