package com.pgaot.account.auth;

import com.pgaot.account.auth.api.LoginEntry;
import com.pgaot.account.auth.common.model.TokenInfo;
import com.pgaot.account.auth.core.token.scope.Scope;

import java.util.List;

/**
 * API Token 全流程测试.
 */
public class ApiTokenTest {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  code-auth API Token 测试");
        System.out.println("==========================================\n");

        String user = "alice";

        // 清理旧 token
        try {
            for (TokenInfo old : LoginEntry.tokens().list(user))
                LoginEntry.tokens().revoke(user, old.getId());
        } catch (Exception ignored) {}

        // ════════════════════════
        // 1. 创建令牌
        // ════════════════════════
        pause();
        System.out.println("  === 1. 创建令牌 ===\n");

        print("1a. 创建数据表令牌");
        TokenInfo t1 = LoginEntry.tokens().create(user, "数据表访问",
                List.of(Scope.Datasheet.DATA));
        String token = t1.getToken();
        check(t1.getToken() != null && t1.getToken().startsWith("pat_"), "token 格式不对");
        System.out.println("    令牌: " + t1.getPrefix() + "...（仅此可见完整值）");

        print("1b. 创建超级管理员令牌");
        TokenInfo t2 = LoginEntry.tokens().create(user, "超级管理员",
                List.of(Scope.SUPER));
        String superToken = t2.getToken();
        System.out.println("    令牌: " + t2.getPrefix() + "...");

        // ════════════════════════
        // 2. 校验
        // ════════════════════════
        pause();
        System.out.println("  === 2. 校验令牌 ===\n");

        print("2a. 数据表令牌 + datasheet:data");
        try {
            String uid = LoginEntry.tokens().validate(token, "datasheet:data");
            check(uid.equals(user), "userId 不对: " + uid);
        } catch (Exception e) {
            check(false, "应通过: " + e.getMessage());
        }

        print("2b. 数据表令牌 + storage:upload（无此 scope）");
        try {
            LoginEntry.tokens().validate(token, "storage:upload");
            check(false, "应被拒绝");
        } catch (Exception e) {
            check(true, "拦截");
        }

        print("2c. 超级管理员令牌 + storage:upload");
        try {
            LoginEntry.tokens().validate(superToken, "storage:upload");
            check(true, "SUPER 应通过任何 scope");
        } catch (Exception e) {
            check(false, "不应拦截: " + e.getMessage());
        }

        print("2d. 伪造 token");
        try {
            LoginEntry.tokens().validate("pat_fake_token_1234567890abcdef", "datasheet:data");
            check(false, "伪造 token 应被拒绝");
        } catch (Exception e) {
            check(true, "拦截: " + e.getMessage());
        }

        // ════════════════════════
        // 3. 管理
        // ════════════════════════
        pause();
        System.out.println("  === 3. 管理令牌 ===\n");

        print("3a. 列出用户所有 token");
        List<TokenInfo> list = LoginEntry.tokens().list(user);
        check(list.size() == 2, "应有 2 个 token: " + list.size());
        for (TokenInfo ti : list)
            System.out.println("    [" + ti.getId() + "] " + ti.getName() + " " + ti.getPrefix() + "...");

        print("3b. 吊销一个 token");
        LoginEntry.tokens().revoke(user, t2.getId());

        print("3c. 吊销后校验 — 应拒绝");
        try {
            LoginEntry.tokens().validate(superToken, "datasheet:data");
            check(false, "吊销后应被拒");
        } catch (Exception e) {
            check(true, "拦截: " + e.getMessage());
        }

        print("3d. 吊销后列表只剩 1 个");
        List<TokenInfo> after = LoginEntry.tokens().list(user);
        check(after.size() == 1, "应剩 1 个: " + after.size());

        // 清理
        for (TokenInfo old : LoginEntry.tokens().list(user))
            LoginEntry.tokens().revoke(user, old.getId());

        System.out.println("\n==========================================");
        System.out.println("  总计: " + (pass + fail) + " | PASS: " + pass + " | FAIL: " + fail);
        System.out.println("==========================================");
        if (fail > 0) System.exit(1);
    }

    static void print(String msg) { System.out.print("  " + msg + " ... "); }

    static void check(boolean ok, String detail) {
        if (ok) { System.out.println("PASS"); pass++; }
        else    { System.out.println("FAIL — " + detail); fail++; }
    }

    static void pause() {
        System.out.print("    按回车继续...");
        try { System.in.read(); } catch (Exception ignored) {}
        System.out.println();
    }
}
