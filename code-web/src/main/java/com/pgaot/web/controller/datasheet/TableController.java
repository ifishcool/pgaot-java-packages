package com.pgaot.web.controller.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.ColumnInfo;
import com.pgaot.datasheet.common.model.ColumnType;
import com.pgaot.datasheet.common.model.TableInfo;
import com.pgaot.datasheet.common.model.TableMode;
import com.pgaot.web.common.ApiResponse;
import com.pgaot.web.controller.BaseController;
import com.pgaot.web.param.datasheet.CreateTableRequest;
import com.pgaot.web.param.datasheet.SetModeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("resource")
@Tag(name = "表管理")
@RestController
@RequestMapping("/api/tables")
public class TableController extends BaseController {

    private volatile DatasheetEngine engine;
    private DatasheetEngine engine() {
        if (engine == null) {
            synchronized (this) { if (engine == null) engine = DatasheetEngine.fromEnv(); }
        }
        return engine;
    }

    @Operation(summary = "列出用户表")
    @GetMapping
    public ApiResponse<List<TableInfo>> list(@RequestParam("userId") String userId) {
        return ApiResponse.ok(engine().tables().list(userId));
    }

    @Operation(summary = "建表")
    @PostMapping
    public ApiResponse<TableInfo> create(@RequestParam("userId") String userId,
                                          @RequestBody CreateTableRequest body) {
        List<ColumnInfo> columns = body.getColumns() != null
                ? body.getColumns().stream().map(c ->
                        new ColumnInfo(c.getName(), ColumnType.valueOf(c.getType()), c.isRequired()))
                .toList() : null;
        return ApiResponse.ok(engine().tables().create(userId,
                body.getName(), body.getTitle(), body.getDescription(), columns));
    }

    @Operation(summary = "删表")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> drop(@RequestParam("userId") String userId, @PathVariable("id") String id) {
        engine().tables().drop(userId, id);
        return ApiResponse.ok();
    }

    @Operation(summary = "设置表模式")
    @PutMapping("/{id}/mode")
    public ApiResponse<Void> setMode(@RequestParam("userId") String userId, @PathVariable("id") String id,
                                      @RequestBody SetModeRequest body) {
        engine().tables().setMode(userId, id, TableMode.valueOf(body.getMode()));
        return ApiResponse.ok();
    }

    @Operation(summary = "清空表")
    @PostMapping("/{id}/truncate")
    public ApiResponse<Void> truncate(@RequestParam("userId") String userId, @PathVariable("id") String id) {
        engine().tables().truncate(userId, id);
        return ApiResponse.ok();
    }

    @PreDestroy
    void destroy() { if (engine != null) engine.close(); }
}
