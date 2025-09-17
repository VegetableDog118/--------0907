package com.powertrading.interfaces.controller;

import com.powertrading.interfaces.dto.MysqlTableInfo;
import com.powertrading.interfaces.dto.TableStructureInfo;
import com.powertrading.interfaces.service.MysqlTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * MySQL表管理控制器
 * 根据PRD文档2.0简化的MySQL表选择功能
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mysql")
@Tag(name = "MySQL表管理", description = "MySQL表选择和结构查询相关接口")
@Validated
public class MysqlTableController {

    @Autowired
    private MysqlTableService mysqlTableService;

    /**
     * 获取MySQL表列表
     * 根据PRD文档2.0简化：直接从MySQL数据库获取结构化表列表
     */
    @GetMapping("/tables")
    @Operation(summary = "获取MySQL表列表", description = "从MySQL数据库中获取所有结构化表的列表")
    // @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<List<MysqlTableInfo>> getMysqlTables() {
        try {
            List<MysqlTableInfo> tables = mysqlTableService.getAllTables();
            log.info("获取MySQL表列表成功，共{}个表", tables.size());
            return ApiResponse.success(tables);
        } catch (Exception e) {
            log.error("获取MySQL表列表失败", e);
            return ApiResponse.error("GET_MYSQL_TABLES_FAILED", "获取MySQL表列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取表结构信息
     * 根据PRD文档2.0：支持表结构预览功能
     */
    @GetMapping("/table-structure/{tableName}")
    @Operation(summary = "获取表结构信息", description = "获取指定MySQL表的详细结构信息")
    // @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<TableStructureInfo> getTableStructure(
            @PathVariable @NotBlank(message = "表名不能为空") String tableName) {
        try {
            // 验证表是否存在
            if (!mysqlTableService.tableExists(tableName)) {
                return ApiResponse.error("TABLE_NOT_EXISTS", "选择的表不存在或已删除");
            }

            TableStructureInfo structure = mysqlTableService.getTableStructure(tableName);
            log.info("获取表结构成功，表名: {}, 字段数: {}", tableName, structure.getColumns().size());
            return ApiResponse.success(structure);
        } catch (Exception e) {
            log.error("获取表结构失败，表名: {}", tableName, e);
            return ApiResponse.error("GET_TABLE_STRUCTURE_FAILED", "获取表结构失败: " + e.getMessage());
        }
    }

    /**
     * 验证表是否存在
     */
    @GetMapping("/table-exists/{tableName}")
    @Operation(summary = "验证表是否存在", description = "检查指定的MySQL表是否存在")
    // @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<Boolean> checkTableExists(
            @PathVariable @NotBlank(message = "表名不能为空") String tableName) {
        try {
            boolean exists = mysqlTableService.tableExists(tableName);
            return ApiResponse.success(exists, exists ? "表存在" : "表不存在");
        } catch (Exception e) {
            log.error("检查表是否存在失败，表名: {}", tableName, e);
            return ApiResponse.error("CHECK_TABLE_EXISTS_FAILED", "检查表是否存在失败: " + e.getMessage());
        }
    }

    /**
     * 获取表的记录数统计
     */
    @GetMapping("/table-stats/{tableName}")
    @Operation(summary = "获取表统计信息", description = "获取指定MySQL表的记录数等统计信息")
    // @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<TableStats> getTableStats(
            @PathVariable @NotBlank(message = "表名不能为空") String tableName) {
        try {
            if (!mysqlTableService.tableExists(tableName)) {
                return ApiResponse.error("TABLE_NOT_EXISTS", "选择的表不存在或已删除");
            }

            TableStats stats = mysqlTableService.getTableStats(tableName);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取表统计信息失败，表名: {}", tableName, e);
            return ApiResponse.error("GET_TABLE_STATS_FAILED", "获取表统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 测试MySQL连接
     */
    @GetMapping("/connection/test")
    @Operation(summary = "测试MySQL连接", description = "测试MySQL数据库连接状态")
    // @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<Boolean> testMysqlConnection() {
        try {
            boolean connected = mysqlTableService.testConnection();
            return ApiResponse.success(connected, connected ? "MySQL连接正常" : "MySQL连接失败");
        } catch (Exception e) {
            log.error("测试MySQL连接失败", e);
            return ApiResponse.error("TEST_MYSQL_CONNECTION_FAILED", "MySQL数据库连接异常，请联系管理员");
        }
    }

    /**
     * 表统计信息类
     */
    public static class TableStats {
        private String tableName;
        private Long recordCount;
        private String tableSize;
        private String lastUpdateTime;
        private String tableEngine;
        private String tableCollation;

        // getters and setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public Long getRecordCount() { return recordCount; }
        public void setRecordCount(Long recordCount) { this.recordCount = recordCount; }
        public String getTableSize() { return tableSize; }
        public void setTableSize(String tableSize) { this.tableSize = tableSize; }
        public String getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(String lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
        public String getTableEngine() { return tableEngine; }
        public void setTableEngine(String tableEngine) { this.tableEngine = tableEngine; }
        public String getTableCollation() { return tableCollation; }
        public void setTableCollation(String tableCollation) { this.tableCollation = tableCollation; }
    }

    /**
     * API响应包装类
     */
    public static class ApiResponse<T> {
        private String code;
        private String message;
        private T data;
        private Boolean success;
        private Long timestamp;

        public static <T> ApiResponse<T> success(T data) {
            return success(data, "操作成功");
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setCode("SUCCESS");
            response.setMessage(message);
            response.setData(data);
            response.setSuccess(true);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }

        public static <T> ApiResponse<T> error(String code, String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setCode(code);
            response.setMessage(message);
            response.setSuccess(false);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }

        // getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}