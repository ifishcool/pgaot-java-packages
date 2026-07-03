package com.pgaot.datasheet;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/** 测试辅助：自动加载模块根目录的 .env 到 System properties */
public final class EnvLoader {

    private static volatile boolean loaded = false;

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
                if (System.getProperty(key) == null) {
                    System.setProperty(key, val);
                }
            }
        } catch (Exception ignored) {}
    }

    public static String get(String key) {
        load();
        String v = System.getenv(key);
        return v != null ? v : System.getProperty(key);
    }

    public static boolean hasDb() {
        load();
        // .env 存在即可（含 _NAME 后缀的配置也视为有效）
        return new File(".env").exists()
                || System.getenv("CODE_SQL_URL") != null
                || System.getenv("CODE_SQL_URL_DATA") != null;
    }
}
