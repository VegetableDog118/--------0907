package com.powertrading.interfaces.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.powertrading.interfaces.common.ApiResponse;
import com.powertrading.interfaces.dto.InterfaceQueryRequest;
import com.powertrading.interfaces.entity.InterfaceCategory;
import com.powertrading.interfaces.entity.InterfaceParameter;
import com.powertrading.interfaces.service.InterfaceManagementService;
import com.powertrading.interfaces.service.MysqlTableService;
import com.powertrading.interfaces.service.InterfaceGenerationService;
import com.powertrading.interfaces.vo.InterfaceVO;
import com.powertrading.interfaces.dto.MysqlTableInfo;
import com.powertrading.interfaces.dto.ColumnInfo;
import com.powertrading.interfaces.dto.InterfaceGenerationRequest;
import com.powertrading.interfaces.dto.InterfaceGenerationResult;
import com.powertrading.interfaces.dto.TableStructureInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口管理控制器
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/interfaces")
@Tag(name = "接口管理", description = "接口配置和参数管理相关API")
@Validated
public class InterfaceManagementController {



    @Autowired
    private InterfaceManagementService interfaceManagementService;
    
    @Autowired
    private MysqlTableService mysqlTableService;
    
    @Autowired
    private InterfaceGenerationService interfaceGenerationService;

    /**
     * 分页查询接口列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询接口列表", description = "支持按名称、分类、状态等条件查询接口")
    public ApiResponse<IPage<InterfaceVO>> getInterfaceList(@Valid InterfaceQueryRequest request,
                                                           @RequestParam(required = false) String status) {
        try {
            // 处理单个status参数
            if (status != null && !status.isEmpty()) {
                if ("published".equals(status)) {
                    request.setPublishedOnly(true);
                } else {
                    request.setStatusList(java.util.Arrays.asList(status));
                }
            }
            
            IPage<InterfaceVO> result = interfaceManagementService.getInterfaceList(request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询接口列表失败", e);
            return ApiResponse.error("查询接口列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取接口详情
     *
     * @param interfaceId 接口ID
     * @return 接口详情
     */
    @GetMapping("/{interfaceId}")
    @Operation(summary = "获取接口详情", description = "根据接口ID获取完整的接口信息，包括参数配置")
    public ApiResponse<InterfaceVO> getInterfaceDetail(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId) {
        try {
            InterfaceVO result = interfaceManagementService.getInterfaceDetail(interfaceId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取接口详情失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("获取接口详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新接口配置
     *
     * @param interfaceId 接口ID
     * @param request 更新请求
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PutMapping("/{interfaceId}/config")
    @Operation(summary = "更新接口配置", description = "更新接口的基本配置信息，如名称、描述、分类等")
    public ApiResponse<Void> updateInterfaceConfig(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            @RequestBody @Valid InterfaceManagementService.InterfaceUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            String updateBy = getUserFromRequest(httpRequest);
            interfaceManagementService.updateInterfaceConfig(interfaceId, request, updateBy);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("更新接口配置失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("更新接口配置失败: " + e.getMessage());
        }
    }

    /**
     * 更新接口参数
     *
     * @param interfaceId 接口ID
     * @param parameters 参数列表
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PutMapping("/{interfaceId}/parameters")
    @Operation(summary = "更新接口参数", description = "更新接口的参数配置，支持批量更新")
    public ApiResponse<Void> updateInterfaceParameters(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            @RequestBody @NotEmpty(message = "参数列表不能为空") 
            List<InterfaceManagementService.InterfaceParameterRequest> parameters,
            HttpServletRequest httpRequest) {
        try {
            String updateBy = getUserFromRequest(httpRequest);
            interfaceManagementService.updateInterfaceParameters(interfaceId, parameters, updateBy);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("更新接口参数失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("更新接口参数失败: " + e.getMessage());
        }
    }

    /**
     * 删除接口
     *
     * @param interfaceId 接口ID
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @DeleteMapping("/{interfaceId}")
    @Operation(summary = "删除接口", description = "删除指定的接口及其相关配置")
    public ApiResponse<Void> deleteInterface(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            HttpServletRequest httpRequest) {
        try {
            String deleteBy = getUserFromRequest(httpRequest);
            interfaceManagementService.deleteInterface(interfaceId, deleteBy);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("删除接口失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("删除接口失败: " + e.getMessage());
        }
    }

    /**
     * 获取接口分类列表
     *
     * @return 分类列表
     */
    @GetMapping("/categories")
    @Operation(summary = "获取接口分类列表", description = "获取所有可用的接口分类")
    public ApiResponse<List<InterfaceCategory>> getInterfaceCategories() {
        try {
            List<InterfaceCategory> result = interfaceManagementService.getInterfaceCategories();
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取接口分类列表失败", e);
            return ApiResponse.error("获取接口分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取标准参数模板
     *
     * @return 标准参数列表
     */
    @GetMapping("/standard-parameters")
    @Operation(summary = "获取标准参数模板", description = "获取系统预定义的标准参数模板")
    public ApiResponse<List<InterfaceParameter>> getStandardParameterTemplates() {
        try {
            List<InterfaceParameter> result = interfaceManagementService.getStandardParameterTemplates();
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取标准参数模板失败", e);
            return ApiResponse.error("获取标准参数模板失败: " + e.getMessage());
        }
    }

    /**
     * 复制接口
     *
     * @param sourceInterfaceId 源接口ID
     * @param request 复制请求
     * @param httpRequest HTTP请求
     * @return 新接口ID
     */
    @PostMapping("/{sourceInterfaceId}/copy")
    @Operation(summary = "复制接口", description = "基于现有接口创建副本")
    public ApiResponse<String> copyInterface(
            @Parameter(description = "源接口ID", required = true)
            @PathVariable @NotBlank(message = "源接口ID不能为空") String sourceInterfaceId,
            @RequestBody @Valid CopyInterfaceRequest request,
            HttpServletRequest httpRequest) {
        try {
            String createBy = getUserFromRequest(httpRequest);
            String newInterfaceId = interfaceManagementService.copyInterface(
                sourceInterfaceId, request.getNewInterfaceName(), createBy);
            return ApiResponse.success(newInterfaceId);
        } catch (Exception e) {
            log.error("复制接口失败，源接口ID: {}", sourceInterfaceId, e);
            return ApiResponse.error("复制接口失败: " + e.getMessage());
        }
    }

    /**
     * 获取接口统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取接口统计信息", description = "获取接口的统计数据")
    public ApiResponse<Map<String, Object>> getInterfaceStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 这里可以调用相应的统计方法
            // 暂时返回空的统计信息
            statistics.put("totalCount", 0);
            statistics.put("publishedCount", 0);
            statistics.put("unpublishedCount", 0);
            statistics.put("offlineCount", 0);
            
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取接口统计信息失败", e);
            return ApiResponse.error("获取接口统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 验证接口名称是否可用
     *
     * @param interfaceName 接口名称
     * @param excludeId 排除的接口ID（用于更新时验证）
     * @return 验证结果
     */
    @GetMapping("/validate-name")
    @Operation(summary = "验证接口名称", description = "检查接口名称是否已被使用")
    public ApiResponse<Boolean> validateInterfaceName(
            @Parameter(description = "接口名称", required = true)
            @RequestParam @NotBlank(message = "接口名称不能为空") String interfaceName,
            @Parameter(description = "排除的接口ID")
            @RequestParam(required = false) String excludeId) {
        try {
            // 这里应该调用相应的验证方法
            // 暂时返回true表示可用
            boolean available = true;
            return ApiResponse.success(available);
        } catch (Exception e) {
            log.error("验证接口名称失败，接口名称: {}", interfaceName, e);
            return ApiResponse.error("验证接口名称失败: " + e.getMessage());
        }
    }

    // ========== MySQL表选择相关接口 ==========

    /**
     * 获取MySQL表列表
     *
     * @return MySQL表列表
     */
    @GetMapping("/mysql-tables")
    @Operation(summary = "获取MySQL表列表", description = "从MySQL数据库中获取所有结构化表")
    public ApiResponse<List<MysqlTableInfo>> getMysqlTables() {
        try {
            List<MysqlTableInfo> tables = mysqlTableService.getAllTables();
            return ApiResponse.success(tables);
        } catch (Exception e) {
            log.error("获取MySQL表列表失败", e);
            return ApiResponse.error("获取MySQL表列表失败: " + e.getMessage());
        }
    }

    /**
      * 获取表结构信息
      *
      * @param tableName 表名
      * @return 表结构信息
      */
     @GetMapping("/table-structure/{tableName}")
     @Operation(summary = "获取表结构信息", description = "获取指定MySQL表的字段结构信息")
     public ApiResponse<TableStructureInfo> getTableStructure(
             @Parameter(description = "表名", required = true)
             @PathVariable @NotBlank(message = "表名不能为空") String tableName) {
         try {
             TableStructureInfo structure = mysqlTableService.getTableStructure(tableName);
             return ApiResponse.success(structure);
         } catch (Exception e) {
             log.error("获取表结构失败，表名: {}", tableName, e);
             return ApiResponse.error("获取表结构失败: " + e.getMessage());
         }
     }

     /**
      * 生成接口（MySQL表模式）
      *
      * @param request 接口生成请求
      * @return 生成结果
      */
     @PostMapping("/generate")
     @Operation(summary = "生成接口", description = "基于MySQL表生成接口")
     public ApiResponse<InterfaceGenerationResult> generateInterface(
             @RequestBody @Valid InterfaceGenerationRequest request) {
         try {
             InterfaceGenerationResult result = interfaceGenerationService.generateFromMysqlTable(request);
             if (result.getSuccess()) {
                 return ApiResponse.success(result);
             } else {
                 return ApiResponse.error(result.getMessage());
             }
         } catch (Exception e) {
             log.error("生成接口失败", e);
             return ApiResponse.error("生成接口失败: " + e.getMessage());
         }
     }

    /**
     * 从请求中获取用户信息
     */
    private String getUserFromRequest(HttpServletRequest request) {
        // 从请求头或会话中获取用户信息
        String user = request.getHeader("X-User-Id");
        if (user == null) {
            user = "system"; // 默认用户
        }
        return user;
    }

    /**
     * 复制接口请求
     */
    public static class CopyInterfaceRequest {
        @NotBlank(message = "新接口名称不能为空")
        private String newInterfaceName;
        
        public String getNewInterfaceName() {
            return newInterfaceName;
        }
        
        public void setNewInterfaceName(String newInterfaceName) {
            this.newInterfaceName = newInterfaceName;
        }
    }


}