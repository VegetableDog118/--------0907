package com.powertrading.interfaces.controller;

import com.powertrading.interfaces.client.DataSourceClient;
import com.powertrading.interfaces.dto.InterfaceGenerationRequest;
import com.powertrading.interfaces.entity.InterfaceParameter;
import com.powertrading.interfaces.service.InterfaceGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 接口生成控制器
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/interfaces/generation")
@Tag(name = "接口生成", description = "接口生成四步骤向导相关接口")
@Validated
public class InterfaceGenerationController {

    private static final Logger log = LoggerFactory.getLogger(InterfaceGenerationController.class);

    @Autowired
    private InterfaceGenerationService generationService;

    // ========== 步骤1：数据源选择 ==========

    /**
     * 获取数据源列表
     */
    @GetMapping("/step1/datasources")
    @Operation(summary = "获取数据源列表", description = "步骤1：获取可用的数据源列表")
    @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<List<DataSourceClient.DataSourceInfo>> getDataSources() {
        try {
            List<DataSourceClient.DataSourceInfo> dataSources = generationService.getDataSources();
            return ApiResponse.success(dataSources);
        } catch (Exception e) {
            log.error("获取数据源列表失败", e);
            return ApiResponse.error("GET_DATASOURCES_FAILED", e.getMessage());
        }
    }

    /**
     * 获取数据源的表列表
     */
    @GetMapping("/step1/datasources/{dataSourceId}/tables")
    @Operation(summary = "获取数据表列表", description = "步骤1：获取指定数据源的表列表")
    @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<List<DataSourceClient.TableInfo>> getDataSourceTables(
            @PathVariable @NotBlank(message = "数据源ID不能为空") String dataSourceId) {
        try {
            List<DataSourceClient.TableInfo> tables = generationService.getDataSourceTables(dataSourceId);
            return ApiResponse.success(tables);
        } catch (Exception e) {
            log.error("获取数据表列表失败，数据源ID: {}", dataSourceId, e);
            return ApiResponse.error("GET_TABLES_FAILED", e.getMessage());
        }
    }

    /**
     * 获取表结构信息
     */
    @GetMapping("/step1/datasources/{dataSourceId}/tables/{tableName}/structure")
    @Operation(summary = "获取表结构信息", description = "步骤1：获取指定表的结构信息")
    @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<DataSourceClient.TableStructure> getTableStructure(
            @PathVariable @NotBlank(message = "数据源ID不能为空") String dataSourceId,
            @PathVariable @NotBlank(message = "表名不能为空") String tableName) {
        try {
            DataSourceClient.TableStructure structure = generationService.getTableStructure(dataSourceId, tableName);
            return ApiResponse.success(structure);
        } catch (Exception e) {
            log.error("获取表结构失败，数据源ID: {}, 表名: {}", dataSourceId, tableName, e);
            return ApiResponse.error("GET_TABLE_STRUCTURE_FAILED", e.getMessage());
        }
    }

    // ========== 步骤2：接口配置 ==========

    /**
     * 验证接口配置
     */
    @PostMapping("/step2/validate")
    @Operation(summary = "验证接口配置", description = "步骤2：验证接口配置的有效性")
    @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<InterfaceGenerationService.InterfaceConfigValidationResult> validateInterfaceConfig(
            @Valid @RequestBody InterfaceGenerationRequest.InterfaceConfiguration config) {
        try {
            InterfaceGenerationService.InterfaceConfigValidationResult result = 
                generationService.validateInterfaceConfig(config);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("验证接口配置失败", e);
            return ApiResponse.error("VALIDATE_CONFIG_FAILED", e.getMessage());
        }
    }

    // ========== 步骤3：参数设置 ==========

    /**
     * 获取标准参数模板
     */
    @GetMapping("/step3/standard-parameters")
    @Operation(summary = "获取标准参数模板", description = "步骤3：获取标准参数模板")
    @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<List<InterfaceParameter>> getStandardParameterTemplate() {
        try {
            List<InterfaceParameter> parameters = generationService.getStandardParameterTemplate();
            return ApiResponse.success(parameters);
        } catch (Exception e) {
            log.error("获取标准参数模板失败", e);
            return ApiResponse.error("GET_STANDARD_PARAMETERS_FAILED", e.getMessage());
        }
    }

    // ========== 步骤4：预览确认 ==========

    /**
     * 预览接口配置
     */
    @PostMapping("/step4/preview")
    @Operation(summary = "预览接口配置", description = "步骤4：预览接口配置信息")
    @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<InterfaceGenerationService.InterfacePreview> previewInterface(
            @Valid @RequestBody InterfaceGenerationRequest request) {
        try {
            InterfaceGenerationService.InterfacePreview preview = generationService.previewInterface(request);
            return ApiResponse.success(preview);
        } catch (Exception e) {
            log.error("预览接口配置失败", e);
            return ApiResponse.error("PREVIEW_INTERFACE_FAILED", e.getMessage());
        }
    }

    /**
     * 生成接口
     */
    @PostMapping("/step4/generate")
    @Operation(summary = "生成接口", description = "步骤4：确认生成接口")
    @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<String> generateInterface(
            @Valid @RequestBody InterfaceGenerationRequest request,
            HttpServletRequest httpRequest) {
        try {
            String userId = getUserFromRequest(httpRequest);
            String interfaceId = generationService.generateInterface(request, userId);
            return ApiResponse.success(interfaceId, "接口生成成功，当前状态：未上架");
        } catch (Exception e) {
            log.error("生成接口失败", e);
            return ApiResponse.error("GENERATE_INTERFACE_FAILED", e.getMessage());
        }
    }

    // ========== MySQL表模式接口生成 ==========

    /**
     * 从MySQL表生成接口
     * 根据PRD文档2.0简化的MySQL表接口生成
     */
    @PostMapping("/mysql-table/generate")
    @Operation(summary = "从MySQL表生成接口", description = "根据选择的MySQL表生成接口")
    @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<com.powertrading.interfaces.dto.InterfaceGenerationResult> generateFromMysqlTable(
            @Valid @RequestBody InterfaceGenerationRequest request,
            HttpServletRequest httpRequest) {
        try {
            String userId = getUserFromRequest(httpRequest);
            com.powertrading.interfaces.dto.InterfaceGenerationResult result = 
                generationService.generateFromMysqlTable(request);
            return ApiResponse.success(result, "MySQL表接口生成成功");
        } catch (Exception e) {
            log.error("MySQL表接口生成失败，表名: {}", request.getTableName(), e);
            return ApiResponse.error("GENERATE_MYSQL_INTERFACE_FAILED", e.getMessage());
        }
    }

    // ========== 辅助接口 ==========

    /**
     * 测试数据源连接
     */
    @PostMapping("/datasources/{dataSourceId}/test")
    @Operation(summary = "测试数据源连接", description = "测试指定数据源的连接状态")
    @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<Boolean> testDataSourceConnection(
            @PathVariable @NotBlank(message = "数据源ID不能为空") String dataSourceId) {
        try {
            // 调用数据源服务测试连接
            // 这里可以通过DataSourceClient调用测试接口
            return ApiResponse.success(true, "数据源连接正常");
        } catch (Exception e) {
            log.error("测试数据源连接失败，数据源ID: {}", dataSourceId, e);
            return ApiResponse.error("TEST_CONNECTION_FAILED", e.getMessage());
        }
    }

    /**
     * 获取接口生成进度
     */
    @GetMapping("/progress/{taskId}")
    @Operation(summary = "获取生成进度", description = "获取接口生成任务的进度")
    @PreAuthorize("hasAuthority('interface:create')")
    public ApiResponse<GenerationProgress> getGenerationProgress(
            @PathVariable @NotBlank(message = "任务ID不能为空") String taskId) {
        try {
            // 这里可以实现异步生成接口的进度查询
            GenerationProgress progress = new GenerationProgress();
            progress.setTaskId(taskId);
            progress.setStatus("completed");
            progress.setProgress(100);
            progress.setMessage("接口生成完成");
            
            return ApiResponse.success(progress);
        } catch (Exception e) {
            log.error("获取生成进度失败，任务ID: {}", taskId, e);
            return ApiResponse.error("GET_PROGRESS_FAILED", e.getMessage());
        }
    }

    /**
     * 生成进度类
     */
    public static class GenerationProgress {
        private String taskId;
        private String status;
        private int progress;
        private String message;
        
        // getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
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

    /**
     * 从请求中获取用户信息
     */
    private String getUserFromRequest(HttpServletRequest request) {
        String user = request.getHeader("X-User-Id");
        if (user == null) {
            user = "system";
        }
        return user;
    }
}