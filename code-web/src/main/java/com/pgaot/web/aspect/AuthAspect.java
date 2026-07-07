package com.pgaot.web.aspect;

import com.pgaot.account.auth.api.LoginEntry;
import com.pgaot.account.auth.api.model.LoginUser;
import com.pgaot.web.annotation.RequiredScope;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Order(1)

public class AuthAspect {

    @Before("@within(com.pgaot.web.annotation.RequiredAuth) || " +
            "@annotation(com.pgaot.web.annotation.RequiredAuth)")
    public void checkAuth() {
        String token = extractBearer();
        if (token == null) throw new AuthException(401, "未登录");
        LoginUser user = LoginEntry.validate(token);
        setAttr("userId", user.getUserId());
        setAttr("nickname", user.getString("nickname"));
        setAttr("avatar", user.getString("avatar"));
    }

    @Before("@within(com.pgaot.web.annotation.RequiredScope) || " +
            "@annotation(com.pgaot.web.annotation.RequiredScope)")
    public void checkScope(org.aspectj.lang.JoinPoint jp) {
        RequiredScope scope = getAnnotation(jp, RequiredScope.class);
        if (scope == null) return;
        HttpServletRequest req = getRequest();
        String apiKey = req != null ? req.getHeader("X-API-Key") : null;
        if (apiKey == null) throw new AuthException(401, "缺少 API Key");
        LoginEntry.tokens().validate(apiKey, scope.value());
    }

    private String extractBearer() {
        HttpServletRequest req = getRequest();
        String h = req != null ? req.getHeader("Authorization") : null;
        return (h != null && h.startsWith("Bearer ")) ? h.substring(7) : null;
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private void setAttr(String k, Object v) {
        HttpServletRequest req = getRequest();
        if (req != null) req.setAttribute(k, v);
    }

    private <T extends java.lang.annotation.Annotation> T getAnnotation(
            org.aspectj.lang.JoinPoint jp, Class<T> type) {
        // check method first, then class
        java.lang.reflect.Method method =
                ((org.aspectj.lang.reflect.MethodSignature) jp.getSignature()).getMethod();
        T a = method.getAnnotation(type);
        return a != null ? a : jp.getTarget().getClass().getAnnotation(type);
    }

    public static class AuthException extends RuntimeException {
        private final int status;
        public AuthException(int status, String msg) { super(msg); this.status = status; }
        public int getStatus() { return status; }
    }
}
