package com.pgaot.account.auth;

import com.pgaot.account.auth.api.LoginType;
import com.pgaot.account.auth.api.LoginEntry;
import com.pgaot.account.auth.api.model.LoginResult;
import com.pgaot.account.auth.api.model.LoginUser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.awt.Desktop;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * OAuth 登录集成测试 — 需要人工在浏览器中授权.
 * 本地运行: 用 IDE 直接跑 main() 方法.
 * CI 环境: 自动跳过.
 */
@Tag("integration")
@Disabled("需要人工浏览器授权，CI 跳过")
class LoginTest {

    private static final int PORT = 9999;

    public static void main(String[] args) throws Exception {
        String appId = System.getenv("YUNTOWER_APP_ID");
        if (appId == null) { System.err.println("请设置 YUNTOWER_APP_ID"); return; }

        String state = UUID.randomUUID().toString();
        String redirectUri = "http://localhost:" + PORT + "/callback";
        CountDownLatch latch = new CountDownLatch(1);
        String[] codeHolder = new String[1];
        String[] errorHolder = new String[1];

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String status = param(query, "status");
            String code = param(query, "code");
            if ("success".equals(status) && code != null && state.equals(param(query, "state"))) {
                codeHolder[0] = code;
            } else {
                errorHolder[0] = "授权失败";
            }
            byte[] bytes = "<h1>完成，窗口可关闭</h1>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            latch.countDown();
        });
        server.start();

        String scope = URLEncoder.encode("user:profile,user:email", StandardCharsets.UTF_8);
        String authUrl = "https://account.yuntower.com/auth/app?type=redirect"
                + "&appid=" + appId + "&scope=" + scope
                + "&redirect_url=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&state=" + state;

        System.out.println("正在打开浏览器...");
        try { Desktop.getDesktop().browse(new URI(authUrl)); } catch (Exception ignored) {}
        System.out.println("等待授权...");

        boolean ok = latch.await(5, TimeUnit.MINUTES);
        server.stop(0);

        if (!ok || codeHolder[0] == null) {
            System.err.println("失败: " + (errorHolder[0] != null ? errorHolder[0] : "未收到回调"));
            return;
        }

        String code = codeHolder[0];
//        LoginResult result = LoginEntry.login(LoginType.YUNTOWER, Map.of("code", code));
//
//        if (result.isSuccess()) {
//            System.out.println("登录成功: " + result.getUserId() + " " + result.getNickname());
//            LoginUser user = LoginEntry.validate(result.getAccessToken());
//            System.out.println("校验通过: " + user.getUserId());
//            LoginEntry.logout(result.getAccessToken());
//            System.out.println("退出成功");
//        } else {
//            System.err.println("登录失败: [" + result.getCode() + "] " + result.getMessage());
//        }
    }

    @Test
    void placeholderForJUnitDiscovery() {
        // 此测试通过 main() 方法手动运行
        assertTrue(true);
    }

    private static String param(String query, String key) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
        }
        return null;
    }
}
