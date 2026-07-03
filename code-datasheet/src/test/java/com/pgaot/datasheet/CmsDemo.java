package com.pgaot.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.*;

import java.util.List;
import java.util.Map;

/**
 * 内容管理系统（CMS）场景模拟.
 *
 * <p>角色:
 * <ul>
 *   <li>admin: 站点管理员，建表、设权限</li>
 *   <li>editor: 编辑，可新建/修改文章</li>
 *   <li>reviewer: 审核，只读 + 标记状态</li>
 * </ul>
 */
public class CmsDemo {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        DatasheetEngine engine = DatasheetEngine.fromEnv("DATA");

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║     code-datasheet CMS 场景模拟                   ║");
        System.out.println("║     内容管理 — 撰稿→审核→发布→归档               ║");
        System.out.println("╚══════════════════════════════════════════════════╝\n");

        String admin    = "cms_admin";
        String editor   = "cms_editor";
        String reviewer = "cms_reviewer";
        String articleId, mediaId;

        for (String u : List.of(admin, editor, reviewer))
            for (TableInfo old : engine.tables().list(u))
                try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}

        // ════════════════════════════════════
        // 站点搭建
        // ════════════════════════════════════
        pause();
        section("站点搭建：Admin 创建内容表");

        step("1. 创建文章表 [title, content(TEXT), author, status, published_at(TIMESTAMP)]");
        TableInfo ai = engine.tables().create(admin, "articles", "文章表", null, List.of(
                new ColumnInfo("title",        ColumnType.STRING,    true),
                new ColumnInfo("content",      ColumnType.TEXT,      false),
                new ColumnInfo("author",       ColumnType.STRING,    false),
                new ColumnInfo("status",       ColumnType.STRING,    false),
                new ColumnInfo("published_at", ColumnType.TIMESTAMP, false)
        ));
        articleId = ai.getId();

        step("2. 创建媒体表 [filename, url, size(INT), uploader]");
        TableInfo mi = engine.tables().create(admin, "media", "媒体库", null, List.of(
                new ColumnInfo("filename", ColumnType.STRING, true),
                new ColumnInfo("url",      ColumnType.STRING, false),
                new ColumnInfo("size",     ColumnType.INT,    false),
                new ColumnInfo("uploader", ColumnType.STRING, false)
        ));
        mediaId = mi.getId();

        // ════════════════════════════════════
        // 权限分配
        // ════════════════════════════════════
        pause();
        section("权限分配");

        step("3. Admin 共享文章表给 Editor（可插可改，不可删）");
        engine.shares().share(admin, articleId, editor,
                new SharePermission(true, true, true, false));

        step("4. Admin 共享媒体表给 Editor（全部权限）");
        engine.shares().share(admin, mediaId, editor, SharePermission.ALL);

        step("5. Admin 共享文章表给 Reviewer（只读+可改状态）");
        engine.shares().share(admin, articleId, reviewer,
                new SharePermission(true, false, true, false));

        // ════════════════════════════════════
        // 日常撰稿
        // ════════════════════════════════════
        pause();
        section("日常撰稿：Editor 写文章 + 上传素材");

        step("6. Editor 写 3 篇文章（草稿）");
        engine.data().sql(editor,
                "INSERT INTO articles (title,content,author,status) VALUES ('产品发布','产品V2已上线...','张三','draft')");
        engine.data().sql(editor,
                "INSERT INTO articles (title,content,author,status) VALUES ('技术分享','Java 21新特性...','李四','draft')");
        engine.data().sql(editor,
                "INSERT INTO articles (title,content,author,status) VALUES ('行业观察','2026年AI趋势...','张三','draft')");
        List<Map<String, Object>> r6 = engine.data().sql(editor,
                "SELECT id,title,author,status FROM articles");
        check(r6.size() == 3, "应有 3 篇");
        printRows(r6);

        step("7. Editor 上传 2 个素材");
        engine.data().sql(editor,
                "INSERT INTO media (filename,url,size,uploader) VALUES ('banner.png','/cdn/banner.png',2048,'张三')");
        engine.data().sql(editor,
                "INSERT INTO media (filename,url,size,uploader) VALUES ('doc.pdf','/cdn/doc.pdf',10240,'李四')");

        step("8. Editor 修改文章状态为待审核");
        engine.data().sql(editor,
                "UPDATE articles SET status='pending' WHERE status='draft'");
        List<Map<String, Object>> r8 = engine.data().sql(editor,
                "SELECT status,COUNT(*) AS cnt FROM articles GROUP BY status");
        printRows(r8);
        check(r8.size() == 1, "应全部变为 pending");

        // ════════════════════════════════════
        // 审核流程
        // ════════════════════════════════════
        pause();
        section("审核流程：Reviewer 审核 + 发布");

        step("9. Reviewer 查看文章列表");
        List<Map<String, Object>> r9 = engine.data().sql(reviewer,
                "SELECT id,title,author,status FROM articles WHERE status='pending'");
        printRows(r9);
        check(r9.size() >= 2, "应有待审核文章");

        step("10. Reviewer 审核通过 2 篇（改状态 + 填发布时间）");
        engine.data().sql(reviewer,
                "UPDATE articles SET status='published', published_at=NOW() WHERE id IN (1,3)");
        engine.data().sql(reviewer,
                "UPDATE articles SET status='rejected' WHERE id=2");

        step("11. Reviewer 尝试 INSERT — 无权限被拦截");
        try {
            engine.data().sql(reviewer, "INSERT INTO articles (title,content,author,status) VALUES ('hack','x','x','draft')");
            check(false, "应被拒绝");
        } catch (Exception e) {
            check(true, "拦截");
        }

        step("12. Reviewer 尝试删文章 — 无 DELETE 权限被拦截");
        try {
            engine.data().sql(reviewer, "DELETE FROM articles WHERE id=2");
            check(false, "应被拒绝");
        } catch (Exception e) {
            check(true, "拦截");
        }

        // ════════════════════════════════════
        // 发布后保护
        // ════════════════════════════════════
        pause();
        section("发布保护：已发布文章设为只读");

        step("13. Admin 将文章表设为 READ_ONLY（防止已发布内容被篡改）");
        engine.tables().setMode(admin, articleId, TableMode.READ_ONLY);

        step("14. Editor 尝试改已发布文章 — 只读拒绝");
        try {
            engine.data().sql(editor, "UPDATE articles SET title='modified' WHERE status='published'");
            check(false, "只读表应拒绝");
        } catch (Exception e) {
            check(true, "拦截");
        }

        step("15. Editor 仍可上传新素材（媒体表未锁）");
        engine.data().sql(editor,
                "INSERT INTO media (filename,url,size,uploader) VALUES ('hero.jpg','/cdn/hero.jpg',4096,'张三')");
        check(true, "媒体表未锁，上传成功");

        // ════════════════════════════════════
        // 数据导出 + 归档
        // ════════════════════════════════════
        pause();
        section("月末归档：导出 + 软删除");

        step("16. 导出文章 JSON（只导出已发布）");
        String json = engine.data().exportJson(admin, articleId, null, "status='published'");
        check(json.contains("产品发布"), "JSON 应含已发布文章");
        System.out.println("    导出 " + (json.split("\"title\"").length - 1) + " 篇已发布文章");

        step("17. Admin 恢复文章表为可写，删驳回文章");
        engine.tables().setMode(admin, articleId, TableMode.ALL);
        engine.data().sql(admin, "DELETE FROM articles WHERE status='rejected'");

        step("18. Admin 删除旧媒体表");
        engine.tables().drop(admin, mediaId);
        check(engine.tables().list(editor).stream()
                .noneMatch(t -> "media".equals(t.getName())), "媒体表已删除");

        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║  CMS 场景完成: 撰稿→审核→发布保护→归档           ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println("\n  PASS: " + pass + " | FAIL: " + fail);
        if (fail > 0) System.exit(1);
    }

    static void section(String t) { System.out.println("\n  ┌─ " + t); }
    static void step(String m)  { System.out.print("  │ " + m + " ... "); }
    static void check(boolean ok, String d) {
        if (ok) { System.out.println("OK"); pass++; }
        else    { System.out.println("FAIL — " + d); fail++; }
    }
    static void printRows(List<Map<String, Object>> rows) {
        for (var r : rows) System.out.println("      " + r);
    }
    static void pause() {
        System.out.print("\n      按回车继续...");
        try { System.in.read(); } catch (Exception ignored) {}
        System.out.println();
    }
}
