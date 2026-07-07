package com.pgaot.web.controller.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.ColumnInfo;
import com.pgaot.datasheet.common.model.ColumnType;
import com.pgaot.datasheet.common.model.TableInfo;
import com.pgaot.datasheet.common.model.TableMode;
import com.pgaot.web.annotation.RequiredAuth;
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
@RequiredAuth
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
        return ApiResponse.ok(engine().tables().list(getUserId()));
    }

    @Operation(summary = "建表")
    @PostMapping
    public ApiResponse<TableInfo> create(@RequestParam("userId") String userId,
                                          @RequestBody CreateTableRequest body) {
        List<ColumnInfo> columns = body.getColumns() != null
                ? body.getColumns().stream().map(c ->
                        new ColumnInfo(c.getName(), ColumnType.valueOf(c.getType()), c.isRequired()))
                .toList() : null;
        return ApiResponse.ok(engine().tables().create(getUserId(),
                body.getName(), body.getTitle(), body.getDescription(), columns));
    }

    @Operation(summary = "删表")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> drop(@PathVariable("id") String id) {
        engine().tables().drop(getUserId(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "设置表模式")
    @PutMapping("/{id}/mode")
    public ApiResponse<Void> setMode(@PathVariable("id") String id,
                                      @RequestBody SetModeRequest body) {
        engine().tables().setMode(getUserId(), id, TableMode.valueOf(body.getMode()));
        return ApiResponse.ok();
    }

    @Operation(summary = "清空表")
    @PostMapping("/{id}/truncate")
    public ApiResponse<Void> truncate(@PathVariable("id") String id) {
        engine().tables().truncate(getUserId(), id);
        return ApiResponse.ok();
    }

    @PreDestroy
    void destroy() { if (engine != null) engine.close(); }
}
