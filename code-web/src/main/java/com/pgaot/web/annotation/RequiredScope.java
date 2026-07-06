package com.pgaot.web.annotation;

import java.lang.annotation.*;

/**
 * 需要 API Token 权限.
 *
 * <pre>{@code
 * @RequiredScope("datasheet:data")
 * @PostMapping("/tables")
 * public ApiResponse<?> create(...) { }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiredScope {
    String value();
}
