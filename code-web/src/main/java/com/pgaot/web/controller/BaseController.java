package com.pgaot.web.controller;

import com.pgaot.account.auth.api.model.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Controller 基类 — 提供 getUserId / getToken / getClientIp.
 *
 * <p>需要登录的接口加 @RequiredAuth，拦截器自动注入 userId/nickname/avatar 到 request attribute.
 */
public class BaseController {

    protected HttpServletRequest getRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    /** 当前登录用户 ID（需 @RequiredAuth） */
    protected String getUserId() {
        HttpServletRequest req = getRequest();
        return req != null ? (String) req.getAttribute("userId") : null;
    }

    /** 当前登录用户昵称 */
    protected String getNickname() {
        HttpServletRequest req = getRequest();
        return req != null ? (String) req.getAttribute("nickname") : null;
    }

    /** 当前登录用户头像 */
    protected String getAvatar() {
        HttpServletRequest req = getRequest();
        return req != null ? (String) req.getAttribute("avatar") : null;
    }

    /** 客户端 IP */
    protected String getClientIp() {
        HttpServletRequest req = getRequest();
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
