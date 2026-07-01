package com.pgaot.account.auth;

import com.pgaot.account.auth.api.LoginType;
import com.pgaot.account.auth.api.LoginEntry;
import com.pgaot.account.auth.api.model.LoginResult;
import com.pgaot.account.auth.api.model.LoginUser;
import com.sun.net.httpserver.HttpServer;

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

public class LoginTest {

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
            String retState = param(query, "state");
            String msg = param(query, "message");

            String html;
            if ("success".equals(status) && code != null && state.equals(retState)) {
                codeHolder[0] = code;
                html = "<h1>授权成功，窗口可关闭</h1>";
            } else if ("denied".equals(status)) {
                errorHolder[0] = "用户拒绝授权";
                html = "<h1>授权被拒绝</h1>";
            } else if ("failed".equals(status)) {
                errorHolder[0] = msg;
                html = "<h1>授权失败</h1><p>" + (msg != null ? msg : "") + "</p>";
            } else {
                errorHolder[0] = msg;
                html = "<h1>授权失败</h1><p>" + (errorHolder[0] != null ? errorHolder[0] : "") + "</p>";
            }

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
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
        LoginResult result = LoginEntry.login(LoginType.YUNTOWER, Map.of("code", code));

        if (result.isSuccess()) {
            System.out.println("登录成功");
            System.out.println("  userId:   " + result.getUserId());
            System.out.println("  nickname: " + result.getNickname());
            System.out.println("  avatar:   " + result.getAvatar());
            System.out.println("  JWT:      " + result.getAccessToken());

            LoginUser user = LoginEntry.validate(result.getAccessToken());
            System.out.println("校验通过: " + user.getUserId());

            System.out.println("\n现在去查 Redis: redis-cli -a vifanlyrs -n 1 get 'login:token:" + user.getUserId() + "'");
            System.out.println("按回车退出...");
            try { System.in.read(); } catch (Exception ignored) {}

            LoginEntry.logout(result.getAccessToken());
            System.out.println("退出成功");
        } else {
            System.out.println("登录失败: [" + result.getCode() + "] " + result.getMessage());
        }
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
