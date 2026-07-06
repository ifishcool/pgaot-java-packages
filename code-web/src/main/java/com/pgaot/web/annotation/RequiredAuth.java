package com.pgaot.web.annotation;

import java.lang.annotation.*;

/**
 * 需要登录.
 *
 * <pre>{@code
 * @RequiredAuth
 * @GetMapping("/profile")
 * public ApiResponse<?> profile(@RequestAttribute("userId") String userId) { }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiredAuth {
}
