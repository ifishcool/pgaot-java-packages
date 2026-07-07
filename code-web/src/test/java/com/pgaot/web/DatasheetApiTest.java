package com.pgaot.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgaot.web.param.datasheet.*;
import com.pgaot.web.common.ApiResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatasheetApiTest {

    static { EnvLoader.load(); }

    @Autowired private MockMvc mvc;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String USER = "api_test_user";
    private static String tableId;

    @BeforeAll
    static void setUp() {
        EnvLoader.load();
        assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库配置");
        // 清理旧数据
        try (com.pgaot.datasheet.api.DatasheetEngine engine =
                     com.pgaot.datasheet.api.DatasheetEngine.fromEnv()) {
            for (var t : engine.tables().list(USER))
                try { engine.tables().drop(USER, t.getId()); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    // ═══════════════ 建表 ═══════════════

    @Test @Order(1)
    void createTable() throws Exception {
        CreateTableRequest req = new CreateTableRequest();
        req.setName("products");
        req.setTitle("产品表");
        req.setDescription("API 测试产品");
        ColumnParam col1 = new ColumnParam(); col1.setName("name"); col1.setType("STRING"); col1.setRequired(true);
        ColumnParam col2 = new ColumnParam(); col2.setName("price"); col2.setType("DECIMAL"); col2.setRequired(false);
        ColumnParam col3 = new ColumnParam(); col3.setName("stock"); col3.setType("INT"); col3.setRequired(false);
        req.setColumns(List.of(col1, col2, col3));

        String resp = mvc.perform(post("/api/tables")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        tableId = JSON.readTree(resp).get("data").get("id").asText();
    }

    // ═══════════════ 插入 ═══════════════

    @Test @Order(2)
    void insertRows() throws Exception {
        InsertRequest req = new InsertRequest();
        req.setTableId(tableId);
        req.setRows(List.of(
                Map.of("name", "笔记本", "price", 5999, "stock", 10),
                Map.of("name", "显示器", "price", 1999, "stock", 5),
                Map.of("name", "键盘", "price", 299, "stock", 20)));

        mvc.perform(post("/api/data/insert")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ═══════════ 查询 ═══════════

    @Test @Order(3)
    void queryData() throws Exception {
        SqlRequest req = new SqlRequest();
        req.setSql("SELECT * FROM products");

        mvc.perform(post("/api/data/sql")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    // ═══════════════ 更新 ═══════════════

    @Test @Order(4)
    void updateRow() throws Exception {
        UpdateRequest req = new UpdateRequest();
        req.setTableId(tableId);
        req.setWhere("name = '键盘'");
        req.setColumn("price");
        req.setValue("399");

        mvc.perform(post("/api/data/update")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ═══════════════ 验证更新 ═══════════════

    @Test @Order(5)
    void verifyUpdate() throws Exception {
        SqlRequest req = new SqlRequest();
        req.setSql("SELECT * FROM products WHERE name = '键盘'");

        String resp = mvc.perform(post("/api/data/sql")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String price = JSON.readTree(resp).get("data").get(0).get("price").asText();
        org.junit.jupiter.api.Assertions.assertTrue(price.startsWith("399"),
                "价格应为 399: " + price);
    }

    // ═══════════════ 删除 ═══════════════

    @Test @Order(6)
    void deleteRow() throws Exception {
        DeleteRequest req = new DeleteRequest();
        req.setTableId(tableId);
        req.setWhere("name = '显示器'");

        mvc.perform(post("/api/data/delete")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ═══════════════ 验证删除 ═══════════════

    @Test @Order(7)
    void verifyDelete() throws Exception {
        SqlRequest req = new SqlRequest();
        req.setSql("SELECT * FROM products");

        mvc.perform(post("/api/data/sql")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // ═══════════════ 设置模式 ═══════════════

    @Test @Order(8)
    void setTableMode() throws Exception {
        SetModeRequest req = new SetModeRequest();
        req.setMode("READ_ONLY");

        mvc.perform(put("/api/tables/" + tableId + "/mode")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ═══════════════ 导出 CSV/JSON ═══════════════

    @Test @Order(9)
    void exportData() throws Exception {
        mvc.perform(get("/api/data/export/csv")
                        .param("userId", USER)
                        .param("tableId", tableId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isString());

        mvc.perform(get("/api/data/export/json")
                        .param("userId", USER)
                        .param("tableId", tableId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isString());
    }

    // ═══════════════ 列表 ═══════════════

    @Test @Order(10)
    void listTables() throws Exception {
        mvc.perform(get("/api/tables")
                        .param("userId", USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // ═══════════════ 聚合查询 ═══════════════

    @Test @Order(11)
    void aggregateQuery() throws Exception {
        SqlRequest req = new SqlRequest();
        req.setSql("SELECT COUNT(*) AS cnt, AVG(price) AS avg_price, SUM(stock) AS total FROM products");

        mvc.perform(post("/api/data/sql")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].cnt").value(2));
    }

    // ═══════════════ 指定列导出 ═══════════════

    @Test @Order(12)
    void exportSelectedColumns() throws Exception {
        mvc.perform(get("/api/data/export/csv")
                        .param("userId", USER)
                        .param("tableId", tableId)
                        .param("columns", "name,price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(
                        org.hamcrest.Matchers.containsString("笔记本")));
    }

    // ═══════════════ 清空表 ═══════════════

    @Test @Order(13)
    void truncateTable() throws Exception {
        // 先改为 ALL 模式才能清空
        SetModeRequest modeReq = new SetModeRequest();
        modeReq.setMode("ALL");
        mvc.perform(put("/api/tables/" + tableId + "/mode")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(modeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mvc.perform(post("/api/tables/" + tableId + "/truncate")
                        .param("userId", USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        SqlRequest req = new SqlRequest();
        req.setSql("SELECT * FROM products");
        mvc.perform(post("/api/data/sql")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ═══════════ 错误场景 ═══════════

    @Test @Order(14)
    void rejectDuplicateTable() throws Exception {
        CreateTableRequest req = new CreateTableRequest();
        req.setName("products");
        req.setTitle("重复表");
        ColumnParam col = new ColumnParam(); col.setName("x"); col.setType("STRING"); col.setRequired(false);
        req.setColumns(List.of(col));

        mvc.perform(post("/api/tables")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(30001002)); // 表名重复
    }

    @Test @Order(15)
    void rejectInvalidSql() throws Exception {
        SqlRequest req = new SqlRequest();
        req.setSql("DROP TABLE products");

        mvc.perform(post("/api/data/sql")
                        .param("userId", USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(30004002)); // DDL 拦截
    }

    @Test @Order(16)
    void rejectMissingUserId() throws Exception {
        SqlRequest req = new SqlRequest();
        req.setSql("SELECT 1");

        mvc.perform(post("/api/data/sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(req)))
                .andExpect(status().is5xxServerError());
    }

    // ═══════════════ 删表 ═══════════════

    @Test @Order(99)
    void dropTable() throws Exception {
        mvc.perform(delete("/api/tables/" + tableId)
                        .param("userId", USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
