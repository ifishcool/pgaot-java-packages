package com.pgaot.log.annotation;

import java.lang.annotation.*;

/**
 * 标记方法需要自动审计记录.
 *
 * <pre>{@code
 * @Auditable(action = "DELETE", tableName = "scores")
 * public void deleteScore(Long id) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    String action();
    String tableName() default "";
}
