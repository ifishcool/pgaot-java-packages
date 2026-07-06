package com.pgaot.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CmsDemo {

    private static DatasheetEngine engine;
    private static final String ADMIN = "cms_admin";
    private static final String EDITOR = "cms_editor";
    private static final String REVIEWER = "cms_reviewer";
    private static String articleId, mediaId;

    @BeforeAll
    static void setup() {
        assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库");
        engine = DatasheetEngine.fromEnv();
        for (String u : List.of(ADMIN, EDITOR, REVIEWER))
            for (TableInfo old : engine.tables().list(u))
                try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}
    }

    @Test @Order(1) void adminCreatesArticlesTable() {
        TableInfo t = engine.tables().create(ADMIN, "articles", "文章表", null, List.of(
                new ColumnInfo("title", ColumnType.STRING, true),
                new ColumnInfo("content", ColumnType.TEXT, false),
                new ColumnInfo("author", ColumnType.STRING, false),
                new ColumnInfo("status", ColumnType.STRING, false)));
        assertNotNull(t);
        articleId = t.getId();
    }

    @Test @Order(2) void adminCreatesMediaTable() {
        TableInfo t = engine.tables().create(ADMIN, "media", "媒体库", null, List.of(
                new ColumnInfo("filename", ColumnType.STRING, true),
                new ColumnInfo("url", ColumnType.STRING, false),
                new ColumnInfo("size", ColumnType.INT, false)));
        assertNotNull(t);
        mediaId = t.getId();
    }

    @Test @Order(3) void adminSharesArticlesToEditor() {
        engine.shares().share(ADMIN, articleId, EDITOR,
                new SharePermission(true, true, true, false));
        engine.shares().share(ADMIN, mediaId, EDITOR,
                new SharePermission(true, true, false, false));
        assertEquals(2, engine.shares().listSent(ADMIN).size());
    }

    @Test @Order(4) void adminSharesReadOnlyToReviewer() {
        engine.shares().share(ADMIN, articleId, REVIEWER, SharePermission.SELECT_ONLY);
    }

    @Test @Order(5) void editorCanInsertArticle() {
        engine.data().sql(EDITOR, "INSERT INTO articles (title,content,author,status) " +
                "VALUES ('测试文章','内容','editor','draft')");
        var r = engine.data().<List<?>>sql(EDITOR, "SELECT * FROM articles");
        assertEquals(1, r.size());
    }

    @Test @Order(6) void reviewerCanOnlySelect() {
        engine.data().sql(REVIEWER, "SELECT * FROM articles");
        assertThrows(Exception.class, () ->
                engine.data().sql(REVIEWER, "INSERT INTO articles (title) VALUES ('x')"));
    }

    @Test @Order(7) void editorCannotAccessUnshared() {
        // editor 未共享给 articles_secret，应拒绝
        engine.tables().create(ADMIN, "articles_secret", "机密文章", null, List.of(
                new ColumnInfo("title", ColumnType.STRING, true)));
        assertThrows(Exception.class, () ->
                engine.data().sql(EDITOR, "SELECT * FROM articles_secret"));
    }

    @AfterAll
    static void cleanup() {
        if (engine != null)
            for (String u : List.of(ADMIN, EDITOR, REVIEWER))
                for (TableInfo old : engine.tables().list(u))
                    try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}
    }
}
