package com.pgaot.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录响应")
public class LoginVO {
    @Schema(description = "访问令牌")
    private final String accessToken;
    @Schema(description = "刷新令牌")
    private final String refreshToken;
    @Schema(description = "用户 ID")
    private final String userId;
    @Schema(description = "昵称")
    private final String nickname;
    @Schema(description = "头像")
    private final String avatar;
    @Schema(description = "邮箱")
    private final String email;

    public LoginVO(String accessToken, String refreshToken, String userId,
                   String nickname, String avatar, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.nickname = nickname;
        this.avatar = avatar;
        this.email = email;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getAvatar() { return avatar; }
    public String getEmail() { return email; }
}
