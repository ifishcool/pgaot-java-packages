package com.pgaot.web.controller.auth;

import com.pgaot.account.auth.api.LoginEntry;
import com.pgaot.account.auth.api.model.LoginResult;
import com.pgaot.web.annotation.RequiredAuth;
import com.pgaot.web.common.ApiResponse;
import com.pgaot.web.controller.BaseController;
import com.pgaot.web.param.auth.LoginRequest;
import com.pgaot.web.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@RequestBody LoginRequest req) {
        LoginResult r = LoginEntry.login(req.getType(), Map.of("code", req.getCode()));
        if (!r.isSuccess()) return ApiResponse.fail(r.getCode(), r.getMessage());
        return ApiResponse.ok(new LoginVO(
                r.getAccessToken(), r.getRefreshToken(),
                r.getUserId(), r.getNickname(), r.getAvatar(), r.getEmail()));
    }

    @RequiredAuth
    @Operation(summary = "校验 JWT")
    @GetMapping("/validate")
    public ApiResponse<Map<String, String>> validate() {
        return ApiResponse.ok(Map.of(
                "userId", getUserId(),
                "nickname", getNickname()));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String auth) {
        LoginEntry.logout(auth.replace("Bearer ", ""));
        return ApiResponse.ok();
    }
}
