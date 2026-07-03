package com.pgaot.sql;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/** 测试辅助：自动加载模块根目录的 .env 到 System properties */
public final class EnvLoader {

    private static volatile boolean loaded = false;

    /** 确保 .env 已加载（幂等） */
    public static synchronized void load() {
        if (loaded) return;
        loaded = true;

        File envFile = new File(".env");
        if (!envFile.exists()) return;

        try {
            for (String line : Files.readAllLines(envFile.toPath())) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                // 同时设到 System properties（System.getenv 不可写）
                if (System.getProperty(key) == null) {
                    System.setProperty(key, val);
                }
            }
        } catch (Exception ignored) {}
    }

    /** 获取配置值，优先环境变量，回退到 .env 文件 */
    public static String get(String key) {
        load();
        String v = System.getenv(key);
        return v != null ? v : System.getProperty(key);
    }

    /** 检查是否配置了数据库 */
    public static boolean hasDb() {
        load();
        return new File(".env").exists() || System.getenv("CODE_SQL_URL") != null;
    }
}
