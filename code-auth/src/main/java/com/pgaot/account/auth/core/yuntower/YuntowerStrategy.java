package com.pgaot.account.auth.core.yuntower;

import com.pgaot.account.auth.common.constants.Messages;
import com.pgaot.account.auth.core.strategy.LoginStrategy;
import com.pgaot.account.auth.core.strategy.UserInfo;
import com.yuntower.account.sdk.YuntowerAccountClient;
import com.yuntower.account.sdk.model.response.TokenResponse;
import com.yuntower.account.sdk.model.response.UserProfileResponse;

import java.util.Map;
import java.util.function.Function;

/** 云塔登录策略 */
class YuntowerStrategy implements LoginStrategy {

    private final YuntowerAccountClient yuntower;
    private final Function<String, String> uidBinder;

    public YuntowerStrategy(YuntowerAccountClient yuntower, Function<String, String> uidBinder) {
        this.yuntower = yuntower;
        this.uidBinder = uidBinder;
    }

    @Override
    public UserInfo authenticate(Map<String, Object> params) {
        String code = String.valueOf(params.get("code"));
        if (code == null || code.isBlank()) throw new IllegalArgumentException(Messages.AUTH_CODE_EMPTY);

        TokenResponse token = yuntower.auth().getToken(code);
        UserProfileResponse profile = yuntower.user().getUserInfo(token.getAccessToken());

        String localUserId = uidBinder.apply(profile.getUid());
        return new UserInfo(localUserId, profile.getNickname(), profile.getAvatar());
    }
}
