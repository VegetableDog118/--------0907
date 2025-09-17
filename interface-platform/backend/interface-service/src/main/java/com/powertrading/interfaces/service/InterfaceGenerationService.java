package com.powertrading.interfaces.service;

import com.powertrading.interfaces.client.DataSourceClient;
import com.powertrading.interfaces.dto.InterfaceGenerationRequest;
import com.powertrading.interfaces.dto.InterfaceGenerationResult;
import com.powertrading.interfaces.entity.Interface;
import com.powertrading.interfaces.entity.InterfaceParameter;
import com.powertrading.interfaces.mapper.InterfaceMapper;
import com.powertrading.interfaces.mapper.InterfaceParameterMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.powertrading.interfaces.service.MysqlTableService;
import com.powertrading.interfaces.utils.InterfacePathGenerator;
import com.powertrading.interfaces.utils.SqlTemplateGenerator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 接口生成服务
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class InterfaceGenerationService {

    private static final Logger log = LoggerFactory.getLogger(InterfaceGenerationService.class);

    @Autowired
    private InterfaceMapper interfaceMapper;

    @Autowired
    private InterfaceParameterMapper parameterMapper;

    @Autowired
    private DataSourceClient dataSourceClient;

    @Autowired
    private InterfacePathGenerator pathGenerator;

    @Autowired
    private SqlTemplateGenerator sqlTemplateGenerator;
    
    @Autowired
    private MysqlTableService mysqlTableService;

    /**
     * 步骤1：获取数据源列表
     *
     * @return 数据源列表
     */
    public List<DataSourceClient.DataSourceInfo> getDataSources() {
        try {
            DataSourceClient.ApiResponse<List<DataSourceClient.DataSourceInfo>> response = 
                dataSourceClient.getDataSources();
            if (response.getSuccess()) {
                return response.getData();
            } else {
                throw new RuntimeException("获取数据源列表失败: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("获取数据源列表失败", e);
            throw new RuntimeException("获取数据源列表失败: " + e.getMessage());
        }
    }

    /**
     * 步骤1：获取数据源的表列表
     *
     * @param dataSourceId 数据源ID
     * @return 表列表
     */
    public List<DataSourceClient.TableInfo> getDataSourceTables(String dataSourceId) {
        try {
            DataSourceClient.ApiResponse<List<DataSourceClient.TableInfo>> response = 
                dataSourceClient.getTables(dataSourceId);
            if (response.getSuccess()) {
                return response.getData();
            } else {
                throw new RuntimeException("获取数据表列表失败: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("获取数据表列表失败，数据源ID: {}", dataSourceId, e);
            throw new RuntimeException("获取数据表列表失败: " + e.getMessage());
        }
    }

    /**
     * 步骤1：获取表结构信息
     *
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @return 表结构信息
     */
    public DataSourceClient.TableStructure getTableStructure(String dataSourceId, String tableName) {
        try {
            DataSourceClient.ApiResponse<DataSourceClient.TableStructure> response = 
                dataSourceClient.getTableStructure(dataSourceId, tableName);
            if (response.getSuccess()) {
                return response.getData();
            } else {
                throw new RuntimeException("获取表结构失败: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("获取表结构失败，数据源ID: {}, 表名: {}", dataSourceId, tableName, e);
            throw new RuntimeException("获取表结构失败: " + e.getMessage());
        }
    }

    /**
     * 步骤2：验证接口配置
     *
     * @param config 接口配置
     * @return 验证结果
     */
    public InterfaceConfigValidationResult validateInterfaceConfig(
            InterfaceGenerationRequest.InterfaceConfiguration config) {
        
        InterfaceConfigValidationResult result = new InterfaceConfigValidationResult();
        result.setValid(true);
        
        // 检查接口名称是否重复 - 使用MyBatis-Plus查询避免参数绑定问题
        QueryWrapper<Interface> nameQuery = new QueryWrapper<>();
        nameQuery.eq("interface_name", config.getInterfaceName());
        Interface existingByName = interfaceMapper.selectOne(nameQuery);
        if (existingByName != null) {
            result.setValid(false);
            result.addError("接口名称已存在，请修改");
        }
        
        // 生成接口路径
        String interfacePath;
        if (StringUtils.hasText(config.getInterfacePath())) {
            interfacePath = config.getInterfacePath();
        } else {
            interfacePath = pathGenerator.generateInterfacePath(
                config.getCategoryId(), config.getInterfaceName(), "default_table");
        }
        
        // 检查接口路径是否重复 - 使用MyBatis-Plus查询避免参数绑定问题
        QueryWrapper<Interface> pathQuery = new QueryWrapper<>();
        pathQuery.eq("interface_path", interfacePath);
        Interface existingByPath = interfaceMapper.selectOne(pathQuery);
        if (existingByPath != null) {
            result.setValid(false);
            result.addError("接口路径冲突，请调整: " + interfacePath);
        }
        
        result.setGeneratedPath(interfacePath);
        return result;
    }

    /**
     * 步骤3：获取标准参数模板
     *
     * @return 标准参数列表
     */
    public List<InterfaceParameter> getStandardParameterTemplate() {
        List<InterfaceParameter> standardParams = new ArrayList<>();
        
        // dataTime参数
        InterfaceParameter dataTimeParam = new InterfaceParameter();
        dataTimeParam.setParamName("dataTime");
        dataTimeParam.setParamType("string");
        dataTimeParam.setParamLocation("body");
        dataTimeParam.setDescription("查询日期，格式：YYYY-MM-DD");
        dataTimeParam.setRequired(true);
        dataTimeParam.setValidationRule("date:YYYY-MM-DD,max:yesterday");
        dataTimeParam.setExample("2022-03-17");
        dataTimeParam.setSortOrder(1);
        standardParams.add(dataTimeParam);
        
        // appId参数
        InterfaceParameter appIdParam = new InterfaceParameter();
        appIdParam.setParamName("appId");
        appIdParam.setParamType("string");
        appIdParam.setParamLocation("body");
        appIdParam.setDescription("应用ID，用户身份标识");
        appIdParam.setRequired(true);
        appIdParam.setValidationRule("string,length:15-20");
        appIdParam.setExample("KzoHypQZH4-F6qM63L");
        appIdParam.setSortOrder(2);
        standardParams.add(appIdParam);
        
        return standardParams;
    }

    /**
     * 步骤4：预览接口配置
     *
     * @param request 接口生成请求
     * @return 预览信息
     */
    public InterfacePreview previewInterface(InterfaceGenerationRequest request) {
        InterfacePreview preview = new InterfacePreview();
        
        // 基本信息
        preview.setInterfaceName(request.getConfig().getInterfaceName());
        preview.setDescription(request.getConfig().getDescription());
        preview.setRequestMethod(request.getConfig().getRequestMethod());
        
        // 生成接口路径
        String interfacePath = pathGenerator.generateInterfacePath(
            request.getConfig().getCategoryId(),
            request.getConfig().getInterfaceName(), 
            request.getTableName());
        preview.setInterfacePath(interfacePath);
        
        // 参数信息
        List<InterfaceGenerationRequest.ParameterConfiguration> paramConfigs = convertToParameterConfigurations(request.getParameters());
        preview.setParameters(paramConfigs);
        
        // 生成SQL模板
        try {
            String sqlTemplate = sqlTemplateGenerator.generateSqlTemplate(
                request.getTableName(), 
                paramConfigs);
            preview.setSqlTemplate(sqlTemplate);
        } catch (Exception e) {
            log.warn("生成SQL模板失败", e);
            preview.setSqlTemplate("-- SQL模板生成失败: " + e.getMessage());
        }
        
        // 响应格式
        preview.setResponseFormat(generateResponseFormat());
        
        return preview;
    }

    /**
     * 步骤4：生成接口
     *
     * @param request 接口生成请求
     * @param createBy 创建人
     * @return 生成的接口ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String generateInterface(InterfaceGenerationRequest request, String createBy) {
        try {
            // 1. 验证配置
            InterfaceConfigValidationResult validation = validateInterfaceConfig(request.getConfig());
            if (!validation.isValid()) {
                throw new RuntimeException("接口配置验证失败: " + String.join(", ", validation.getErrors()));
            }
            
            // 2. 创建接口记录
            Interface interfaceEntity = new Interface();
            String interfaceId = UUID.randomUUID().toString().replace("-", "");
            interfaceEntity.setId(interfaceId);
            interfaceEntity.setInterfaceName(request.getConfig().getInterfaceName());
            interfaceEntity.setInterfacePath(validation.getGeneratedPath());
            interfaceEntity.setDescription(request.getConfig().getDescription());
            interfaceEntity.setCategoryId(request.getConfig().getCategoryId());
            interfaceEntity.setDataSourceId("mysql-default");
            interfaceEntity.setTableName(request.getTableName());
            interfaceEntity.setRequestMethod(request.getConfig().getRequestMethod());
            interfaceEntity.setStatus(Interface.STATUS_UNPUBLISHED);
            interfaceEntity.setVersion("1.0");
            interfaceEntity.setRateLimit(request.getConfig().getRateLimit());
            interfaceEntity.setTimeout(request.getConfig().getTimeout());
            interfaceEntity.setCreateBy(createBy);
            
            // 生成SQL模板
            List<InterfaceGenerationRequest.ParameterConfiguration> paramConfigs = convertToParameterConfigurations(request.getParameters());
            String sqlTemplate = sqlTemplateGenerator.generateSqlTemplate(
                request.getTableName(), 
                paramConfigs);
            interfaceEntity.setSqlTemplate(sqlTemplate);
            
            // 生成响应格式
            interfaceEntity.setResponseFormat(generateResponseFormat());
            
            interfaceMapper.insert(interfaceEntity);
            
            // 3. 创建参数记录
            List<InterfaceParameter> parameters = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < request.getParameters().size(); i++) {
                InterfaceGenerationRequest.ParameterDefinition paramConfig = request.getParameters().get(i);
                
                InterfaceParameter parameter = new InterfaceParameter();
                parameter.setId(UUID.randomUUID().toString().replace("-", ""));
                parameter.setInterfaceId(interfaceId);
                parameter.setParamName(paramConfig.getName());
                parameter.setParamType(paramConfig.getType());
                parameter.setParamLocation(paramConfig.getLocation());
                parameter.setDescription(paramConfig.getDescription());
                parameter.setRequired(paramConfig.getRequired());
                parameter.setDefaultValue(paramConfig.getDefaultValue());
                parameter.setValidationRule(""); // ParameterDefinition没有validationRule
                parameter.setExample(paramConfig.getDefaultValue()); // 使用defaultValue作为example
                parameter.setSortOrder(i + 1);
                // 手动设置时间字段，因为批量插入不会触发MyBatis-Plus自动填充
                parameter.setCreateTime(now);
                parameter.setUpdateTime(now);
                
                parameters.add(parameter);
            }
            
            if (!parameters.isEmpty()) {
                parameterMapper.batchInsert(parameters);
            }
            
            log.info("接口生成成功，接口ID: {}, 接口名称: {}, 创建人: {}", 
                interfaceId, request.getConfig().getInterfaceName(), createBy);
            
            return interfaceId;
            
        } catch (Exception e) {
            log.error("接口生成失败", e);
            throw new RuntimeException("接口生成失败: " + e.getMessage());
        }
    }

    // ========== MySQL表模式接口生成 ==========

    /**
     * 从MySQL表生成接口（简化版）
     *
     * @param request 接口生成请求
     * @return 接口生成结果
     */
    @Transactional(rollbackFor = Exception.class)
    public InterfaceGenerationResult generateFromMysqlTable(InterfaceGenerationRequest request) {
        try {
            // 1. 验证MySQL表存在
            if (!mysqlTableService.tableExists(request.getTableName())) {
                throw new RuntimeException("选择的表不存在或已删除: " + request.getTableName());
            }
            
            // 2. 生成接口路径
            String interfacePath = generateInterfacePath(
                request.getInterfaceConfig().getName(),
                request.getInterfaceConfig().getBusinessType());
            
            // 3. 创建接口记录
            Interface interfaceEntity = new Interface();
            String interfaceId = UUID.randomUUID().toString().replace("-", "");
            interfaceEntity.setId(interfaceId);
            interfaceEntity.setInterfaceName(request.getInterfaceConfig().getName());
            interfaceEntity.setInterfacePath(interfacePath);
            interfaceEntity.setDescription(request.getInterfaceConfig().getDescription());
            interfaceEntity.setCategoryId(request.getInterfaceConfig().getBusinessType());
            interfaceEntity.setDataSourceId("mysql-default");
            interfaceEntity.setTableName(request.getTableName());
            interfaceEntity.setRequestMethod(request.getInterfaceConfig().getMethod());
            interfaceEntity.setStatus(Interface.STATUS_UNPUBLISHED);
            interfaceEntity.setVersion("1.0");
            interfaceEntity.setRateLimit(request.getInterfaceConfig().getRateLimit());
            interfaceEntity.setTimeout(request.getInterfaceConfig().getTimeout());
            interfaceEntity.setCreateBy("system");
            
            // 4. 生成SQL模板
            String sqlTemplate = generateMysqlSqlTemplate(request.getTableName(), request.getParameters());
            interfaceEntity.setSqlTemplate(sqlTemplate);
            
            // 5. 生成响应格式
            interfaceEntity.setResponseFormat(generateResponseFormat());
            
            interfaceMapper.insert(interfaceEntity);
            
            // 6. 创建参数记录
            if (request.getParameters() != null && !request.getParameters().isEmpty()) {
                List<InterfaceParameter> parameters = new ArrayList<>();
                LocalDateTime now = LocalDateTime.now();
                for (int i = 0; i < request.getParameters().size(); i++) {
                    InterfaceGenerationRequest.ParameterDefinition paramDef = request.getParameters().get(i);
                    
                    InterfaceParameter parameter = new InterfaceParameter();
                    parameter.setId(UUID.randomUUID().toString().replace("-", ""));
                    parameter.setInterfaceId(interfaceId);
                    parameter.setParamName(paramDef.getName());
                    parameter.setParamType(paramDef.getType());
                    parameter.setParamLocation(paramDef.getLocation());
                    parameter.setDescription(paramDef.getDescription());
                    parameter.setRequired(paramDef.getRequired());
                    parameter.setDefaultValue(paramDef.getDefaultValue());
                    parameter.setSortOrder(i + 1);
                    // 手动设置时间字段，因为批量插入不会触发MyBatis-Plus自动填充
                    parameter.setCreateTime(now);
                    parameter.setUpdateTime(now);
                    
                    parameters.add(parameter);
                }
                
                parameterMapper.batchInsert(parameters);
            }
            
            log.info("MySQL表接口生成成功，接口ID: {}, 表名: {}", interfaceId, request.getTableName());
            
            return InterfaceGenerationResult.builder()
                .interfaceId(interfaceId)
                .interfacePath(interfacePath)
                .status(Interface.STATUS_UNPUBLISHED)
                .message("接口生成成功，当前状态：未上架")
                .timestamp(System.currentTimeMillis())
                .success(true)
                .build();
                
        } catch (Exception e) {
            log.error("MySQL表接口生成失败，表名: {}", request.getTableName(), e);
            return InterfaceGenerationResult.builder()
                .success(false)
                .message("接口生成失败: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        }
    }

    /**
     * 生成接口路径
     *
     * @param interfaceName 接口名称
     * @param businessType 业务类型
     * @return 接口路径
     */
    private String generateInterfacePath(String interfaceName, String businessType) {
        // 将接口名称转换为路径格式
        String pathSuffix = convertToApiPath(interfaceName);
        
        // 根据业务分类生成路径
        String categoryPath = getCategoryPath(businessType);
        
        return categoryPath + "/" + pathSuffix;
    }

    /**
     * 转换为API路径格式
     *
     * @param name 名称
     * @return 路径格式
     */
    private String convertToApiPath(String name) {
        return name.toLowerCase()
            .replaceAll("[\\s\\u4e00-\\u9fa5]+", "-") // 替换空格和中文为连字符
            .replaceAll("[^a-z0-9-]", "") // 移除非字母数字和连字符的字符
            .replaceAll("-+", "-") // 合并多个连字符
            .replaceAll("^-|-$", ""); // 移除开头和结尾的连字符
    }

    /**
     * 获取分类路径
     *
     * @param businessType 业务类型
     * @return 分类路径
     */
    private String getCategoryPath(String businessType) {
        switch (businessType) {
            case "day_ahead_spot":
                return "spot";
            case "forecast":
                return "forecast";
            case "ancillary_service":
                return "ancillary";
            case "grid_operation":
                return "grid";
            default:
                return "common";
        }
    }

    /**
     * 转换ParameterDefinition为ParameterConfiguration
     *
     * @param parameterDefinitions 参数定义列表
     * @return 参数配置列表
     */
    private List<InterfaceGenerationRequest.ParameterConfiguration> convertToParameterConfigurations(
            List<InterfaceGenerationRequest.ParameterDefinition> parameterDefinitions) {
        if (parameterDefinitions == null) {
            return new ArrayList<>();
        }
        
        List<InterfaceGenerationRequest.ParameterConfiguration> configurations = new ArrayList<>();
        for (InterfaceGenerationRequest.ParameterDefinition def : parameterDefinitions) {
            InterfaceGenerationRequest.ParameterConfiguration config = new InterfaceGenerationRequest.ParameterConfiguration();
            config.setParamName(def.getName());
            config.setParamType(def.getType());
            config.setParamLocation(def.getLocation());
            config.setRequired(def.getRequired());
            config.setDescription(def.getDescription());
            config.setDefaultValue(def.getDefaultValue());
            configurations.add(config);
        }
        
        return configurations;
    }

    /**
     * 生成MySQL SQL模板
     *
     * @param tableName 表名
     * @param parameters 参数列表
     * @return SQL模板
     */
    private String generateMysqlSqlTemplate(String tableName, List<InterfaceGenerationRequest.ParameterDefinition> parameters) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName);
        
        if (parameters != null && !parameters.isEmpty()) {
            List<String> conditions = new ArrayList<>();
            for (InterfaceGenerationRequest.ParameterDefinition param : parameters) {
                if (!param.getIsStandard() && 
                    ("query".equals(param.getLocation()) || "body".equals(param.getLocation()))) {
                    conditions.add(param.getName() + " = #{" + param.getName() + "}");
                }
            }
            
            if (!conditions.isEmpty()) {
                sql.append(" WHERE ").append(String.join(" AND ", conditions));
            }
        }
        
        return sql.toString();
    }

    /**
     * 生成标准响应格式
     *
     * @return 响应格式JSON字符串
     */
    private String generateResponseFormat() {
        return "{\"status\":\"success\",\"message\":\"查询成功\",\"data\":[],\"timestamp\":\"2024-01-15T10:30:00Z\"}";
    }

    /**
     * 接口配置验证结果
     */
    public static class InterfaceConfigValidationResult {
        private boolean valid;
        private List<String> errors = new ArrayList<>();
        private String generatedPath;
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public void addError(String error) { this.errors.add(error); }
        public String getGeneratedPath() { return generatedPath; }
        public void setGeneratedPath(String generatedPath) { this.generatedPath = generatedPath; }
    }

    /**
     * 接口预览信息
     */
    public static class InterfacePreview {
        private String interfaceName;
        private String description;
        private String interfacePath;
        private String requestMethod;
        private List<InterfaceGenerationRequest.ParameterConfiguration> parameters;
        private String sqlTemplate;
        private String responseFormat;
        
        // getters and setters
        public String getInterfaceName() { return interfaceName; }
        public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getInterfacePath() { return interfacePath; }
        public void setInterfacePath(String interfacePath) { this.interfacePath = interfacePath; }
        public String getRequestMethod() { return requestMethod; }
        public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
        public List<InterfaceGenerationRequest.ParameterConfiguration> getParameters() { return parameters; }
        public void setParameters(List<InterfaceGenerationRequest.ParameterConfiguration> parameters) { this.parameters = parameters; }
        public String getSqlTemplate() { return sqlTemplate; }
        public void setSqlTemplate(String sqlTemplate) { this.sqlTemplate = sqlTemplate; }
        public String getResponseFormat() { return responseFormat; }
        public void setResponseFormat(String responseFormat) { this.responseFormat = responseFormat; }
    }
}