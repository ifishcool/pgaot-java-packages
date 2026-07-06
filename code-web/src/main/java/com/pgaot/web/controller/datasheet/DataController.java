package com.pgaot.web.controller.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.web.common.ApiResponse;
import com.pgaot.web.controller.BaseController;
import com.pgaot.web.param.datasheet.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@SuppressWarnings("resource")
@Tag(name = "数据操作")
@RestController
@RequestMapping("/api/data")
public class DataController extends BaseController {

    private volatile DatasheetEngine engine;
    private DatasheetEngine engine() {
        if (engine == null) {
            synchronized (this) { if (engine == null) engine = DatasheetEngine.fromEnv(); }
        }
        return engine;
    }

    @Operation(summary = "执行 SQL")
    @PostMapping("/sql")
    public ApiResponse<List<Map<String, Object>>> sql(@RequestParam("userId") String userId,
                                                       @RequestBody SqlRequest body) {
        return ApiResponse.ok(engine().data().sql(userId, body.getSql()));
    }

    @Operation(summary = "插入行")
    @PostMapping("/insert")
    public ApiResponse<Void> insert(@RequestParam("userId") String userId,
                                     @RequestBody InsertRequest body) {
        engine().data().insert(userId, body.getTableId(), body.getRows());
        return ApiResponse.ok();
    }

    @Operation(summary = "更新行")
    @PostMapping("/update")
    public ApiResponse<Void> update(@RequestParam("userId") String userId,
                                     @RequestBody UpdateRequest body) {
        engine().data().update(userId, body.getTableId(), body.getWhere(),
                Map.of(body.getColumn(), body.getValue()));
        return ApiResponse.ok();
    }

    @Operation(summary = "删除行")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam("userId") String userId,
                                     @RequestBody DeleteRequest body) {
        engine().data().delete(userId, body.getTableId(), body.getWhere());
        return ApiResponse.ok();
    }

    @Operation(summary = "导出 CSV")
    @GetMapping("/export/csv")
    public ApiResponse<String> exportCsv(@RequestParam("userId") String userId,
                                          @RequestParam("tableId") String tableId,
                                          @RequestParam(name = "columns", required = false) String columns,
                                          @RequestParam(name = "where", required = false) String where) {
        List<String> cols = columns != null ? List.of(columns.split(",")) : null;
        return ApiResponse.ok(engine().data().exportCsv(userId, tableId, cols, where));
    }

    @Operation(summary = "导出 JSON")
    @GetMapping("/export/json")
    public ApiResponse<String> exportJson(@RequestParam("userId") String userId,
                                           @RequestParam("tableId") String tableId,
                                           @RequestParam(name = "columns", required = false) String columns,
                                           @RequestParam(name = "where", required = false) String where) {
        List<String> cols = columns != null ? List.of(columns.split(",")) : null;
        return ApiResponse.ok(engine().data().exportJson(userId, tableId, cols, where));
    }

    @PreDestroy void destroy() { if (engine != null) engine.close(); }
}
