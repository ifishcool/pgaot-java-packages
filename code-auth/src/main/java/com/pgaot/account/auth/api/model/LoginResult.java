package com.pgaot.account.auth.api.model;

/**
 * 登录返回 — 通过 {@link #isSuccess()} 判断成败，成功时取 token 和用户信息，失败时取 code 和 message.
 *
 * <pre>{@code
 * LoginResult r = LoginEntry.login(LoginType.YUNTOWER, Map.of("code", "xxx"));
 * if (r.isSuccess()) {
 *     // 取 r.getAccessToken() / r.getUserId() / r.getNickname()
 * } else {
 *     // 取 r.getCode() / r.getMessage()
 * }
 * }</pre>
 */
public class LoginResult {

    private final boolean success;
    private final int code;
    private final String message;
    private final String accessToken;
    private final String refreshToken;
    private final String userId;
    private final String nickname;
    private final String avatar;
    private final String email;

    /**
     * 登录成功 — 由 LoginService 内部调用.
     */
    public LoginResult(String accessToken, String refreshToken, String userId,
                       String nickname, String avatar, String email) {
        this.success = true;
        this.code = 0;
        this.message = "ok";
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.nickname = nickname;
        this.avatar = avatar;
        this.email = email;
    }

    /**
     * 登录失败 — 由 LoginEntry 捕获异常后调用.
     *
     * @param code    业务错误码，见 {@link com.pgaot.account.auth.common.code.ErrorCode}
     * @param message 错误描述
     */
    public LoginResult(int code, String message) {
        this.success = false;
        this.code = code;
        this.message = message;
        this.accessToken = null;
        this.refreshToken = null;
        this.userId = null;
        this.nickname = null;
        this.avatar = null;
        this.email = null;
    }

    /** 是否登录成功 */
    public boolean isSuccess() { return success; }

    /** 错误码，0 = 成功 */
    public int getCode() { return code; }

    /** 错误消息 */
    public String getMessage() { return message; }

    /** 访问凭证（成功时有效） */
    public String getAccessToken() { return accessToken; }

    /** 刷新凭证（成功时有效） */
    public String getRefreshToken() { return refreshToken; }

    /** 用户唯一标识（成功时有效） */
    public String getUserId() { return userId; }

    /** 昵称（成功时有效） */
    public String getNickname() { return nickname; }

    /** 头像 URL（成功时有效） */
    public String getAvatar() { return avatar; }

    /** 邮箱（成功时有效） */
    public String getEmail() { return email; }
}
