package com.powertrading.interfaces.service;

import com.powertrading.interfaces.client.DataSourceClient;
import com.powertrading.interfaces.entity.Interface;
import com.powertrading.interfaces.entity.InterfaceParameter;
import com.powertrading.interfaces.mapper.InterfaceMapper;
import com.powertrading.interfaces.mapper.InterfaceParameterMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 接口执行服务
 * 负责动态路由注册后的接口执行逻辑
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class InterfaceExecutionService {

    private static final Logger log = LoggerFactory.getLogger(InterfaceExecutionService.class);

    @Autowired
    private InterfaceMapper interfaceMapper;

    @Autowired
    private InterfaceParameterMapper parameterMapper;

    @Autowired
    private DataSourceClient dataSourceClient;

    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 执行接口
     *
     * @param interfaceId 接口ID
     * @param requestParams 请求参数
     * @return 执行结果
     */
    public InterfaceExecutionResult executeInterface(String interfaceId, Map<String, Object> requestParams) {
        try {
            // 获取接口信息
            Interface interfaceInfo = interfaceMapper.selectById(interfaceId);
            if (interfaceInfo == null) {
                throw new RuntimeException("接口不存在");
            }
            
            // 检查接口状态
            if (!Interface.STATUS_PUBLISHED.equals(interfaceInfo.getStatus())) {
                throw new RuntimeException("接口未上架，无法执行");
            }
            
            // 获取接口参数配置
            List<InterfaceParameter> parameters = parameterMapper.selectByInterfaceId(interfaceId);
            
            // 验证请求参数
            validateRequestParameters(requestParams, parameters);
            
            // 构建SQL查询
            String executeSql = buildExecuteSql(interfaceInfo.getSqlTemplate(), requestParams, parameters);
            
            // 执行数据查询
            DataSourceClient.QueryResult queryResult = executeQuery(
                interfaceInfo.getDataSourceId(), executeSql, requestParams);
            
            // 构建响应结果
            InterfaceExecutionResult result = new InterfaceExecutionResult();
            result.setInterfaceId(interfaceId);
            result.setInterfaceName(interfaceInfo.getInterfaceName());
            result.setExecuteTime(LocalDateTime.now().format(DATETIME_FORMATTER));
            result.setSuccess(true);
            result.setData(queryResult.getData());
            result.setTotalCount(queryResult.getTotalCount());
            result.setExecuteSql(executeSql);
            
            // 记录执行日志
            logInterfaceExecution(interfaceInfo, requestParams, result, null);
            
            return result;
            
        } catch (Exception e) {
            log.error("接口执行失败，接口ID: {}", interfaceId, e);
            
            // 构建错误响应
            InterfaceExecutionResult result = new InterfaceExecutionResult();
            result.setInterfaceId(interfaceId);
            result.setExecuteTime(LocalDateTime.now().format(DATETIME_FORMATTER));
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            
            // 记录错误日志
            try {
                Interface interfaceInfo = interfaceMapper.selectById(interfaceId);
                logInterfaceExecution(interfaceInfo, requestParams, result, e);
            } catch (Exception logException) {
                log.warn("记录接口执行日志失败", logException);
            }
            
            throw new RuntimeException("接口执行失败: " + e.getMessage());
        }
    }

    /**
     * 验证请求参数
     */
    private void validateRequestParameters(Map<String, Object> requestParams, List<InterfaceParameter> parameters) {
        if (CollectionUtils.isEmpty(parameters)) {
            return;
        }
        
        for (InterfaceParameter parameter : parameters) {
            String paramName = parameter.getParamName();
            Object paramValue = requestParams.get(paramName);
            
            // 检查必填参数
            if (parameter.getRequired() && (paramValue == null || 
                (paramValue instanceof String && !StringUtils.hasText((String) paramValue)))) {
                throw new RuntimeException("必填参数缺失: " + paramName);
            }
            
            // 参数类型验证
            if (paramValue != null) {
                validateParameterType(paramName, paramValue, parameter.getParamType());
                validateParameterRule(paramName, paramValue, parameter.getValidationRule());
            }
        }
    }

    /**
     * 验证参数类型
     */
    private void validateParameterType(String paramName, Object paramValue, String paramType) {
        try {
            switch (paramType.toLowerCase()) {
                case "string":
                    // 字符串类型不需要特殊验证
                    break;
                case "integer":
                case "int":
                    if (paramValue instanceof String) {
                        Integer.parseInt((String) paramValue);
                    } else if (!(paramValue instanceof Integer)) {
                        throw new RuntimeException("参数类型错误");
                    }
                    break;
                case "long":
                    if (paramValue instanceof String) {
                        Long.parseLong((String) paramValue);
                    } else if (!(paramValue instanceof Long)) {
                        throw new RuntimeException("参数类型错误");
                    }
                    break;
                case "double":
                case "decimal":
                    if (paramValue instanceof String) {
                        Double.parseDouble((String) paramValue);
                    } else if (!(paramValue instanceof Double)) {
                        throw new RuntimeException("参数类型错误");
                    }
                    break;
                case "boolean":
                    if (paramValue instanceof String) {
                        String strValue = ((String) paramValue).toLowerCase();
                        if (!"true".equals(strValue) && !"false".equals(strValue)) {
                            throw new RuntimeException("参数类型错误");
                        }
                    } else if (!(paramValue instanceof Boolean)) {
                        throw new RuntimeException("参数类型错误");
                    }
                    break;
                case "date":
                    if (paramValue instanceof String) {
                        LocalDateTime.parse((String) paramValue + " 00:00:00", DATETIME_FORMATTER);
                    }
                    break;
                case "datetime":
                    if (paramValue instanceof String) {
                        LocalDateTime.parse((String) paramValue, DATETIME_FORMATTER);
                    }
                    break;
                default:
                    // 未知类型，跳过验证
                    break;
            }
        } catch (Exception e) {
            throw new RuntimeException("参数 " + paramName + " 类型错误，期望类型: " + paramType);
        }
    }

    /**
     * 验证参数规则
     */
    private void validateParameterRule(String paramName, Object paramValue, String validationRule) {
        if (!StringUtils.hasText(validationRule)) {
            return;
        }
        
        String strValue = paramValue.toString();
        String[] rules = validationRule.split(",");
        
        for (String rule : rules) {
            rule = rule.trim();
            
            if (rule.startsWith("length:")) {
                validateLengthRule(paramName, strValue, rule);
            } else if (rule.startsWith("range:")) {
                validateRangeRule(paramName, paramValue, rule);
            } else if (rule.startsWith("date:")) {
                validateDateRule(paramName, strValue, rule);
            } else if (rule.startsWith("regex:")) {
                validateRegexRule(paramName, strValue, rule);
            }
        }
    }

    /**
     * 验证长度规则
     */
    private void validateLengthRule(String paramName, String value, String rule) {
        String lengthRule = rule.substring(7); // 去掉 "length:"
        
        if (lengthRule.contains("-")) {
            String[] parts = lengthRule.split("-");
            int minLength = Integer.parseInt(parts[0]);
            int maxLength = Integer.parseInt(parts[1]);
            
            if (value.length() < minLength || value.length() > maxLength) {
                throw new RuntimeException("参数 " + paramName + " 长度必须在 " + minLength + "-" + maxLength + " 之间");
            }
        } else {
            int expectedLength = Integer.parseInt(lengthRule);
            if (value.length() != expectedLength) {
                throw new RuntimeException("参数 " + paramName + " 长度必须为 " + expectedLength);
            }
        }
    }

    /**
     * 验证范围规则
     */
    private void validateRangeRule(String paramName, Object value, String rule) {
        String rangeRule = rule.substring(6); // 去掉 "range:"
        String[] parts = rangeRule.split("-");
        
        double minValue = Double.parseDouble(parts[0]);
        double maxValue = Double.parseDouble(parts[1]);
        double numValue = Double.parseDouble(value.toString());
        
        if (numValue < minValue || numValue > maxValue) {
            throw new RuntimeException("参数 " + paramName + " 值必须在 " + minValue + "-" + maxValue + " 之间");
        }
    }

    /**
     * 验证日期规则
     */
    private void validateDateRule(String paramName, String value, String rule) {
        String dateRule = rule.substring(5); // 去掉 "date:"
        
        if (dateRule.contains("max:yesterday")) {
            LocalDateTime inputDate = LocalDateTime.parse(value + " 00:00:00", DATETIME_FORMATTER);
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            
            if (inputDate.isAfter(yesterday)) {
                throw new RuntimeException("参数 " + paramName + " 日期不能晚于昨天");
            }
        }
    }

    /**
     * 验证正则表达式规则
     */
    private void validateRegexRule(String paramName, String value, String rule) {
        String regex = rule.substring(6); // 去掉 "regex:"
        
        if (!value.matches(regex)) {
            throw new RuntimeException("参数 " + paramName + " 格式不正确");
        }
    }

    /**
     * 构建执行SQL
     */
    private String buildExecuteSql(String sqlTemplate, Map<String, Object> requestParams, 
                                 List<InterfaceParameter> parameters) {
        String executeSql = sqlTemplate;
        
        // 替换参数占位符
        Matcher matcher = PARAM_PATTERN.matcher(sqlTemplate);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object paramValue = requestParams.get(paramName);
            
            if (paramValue != null) {
                // 根据参数类型进行格式化
                String formattedValue = formatParameterValue(paramValue, paramName, parameters);
                executeSql = executeSql.replace("{" + paramName + "}", formattedValue);
            } else {
                // 参数为空时的处理
                executeSql = executeSql.replace("{" + paramName + "}", "NULL");
            }
        }
        
        return executeSql;
    }

    /**
     * 格式化参数值
     */
    private String formatParameterValue(Object paramValue, String paramName, List<InterfaceParameter> parameters) {
        // 查找参数配置
        InterfaceParameter paramConfig = parameters.stream()
            .filter(p -> p.getParamName().equals(paramName))
            .findFirst()
            .orElse(null);
        
        if (paramConfig == null) {
            return "'" + paramValue.toString() + "'";
        }
        
        String paramType = paramConfig.getParamType().toLowerCase();
        
        switch (paramType) {
            case "string":
            case "date":
            case "datetime":
                return "'" + paramValue.toString() + "'";
            case "integer":
            case "int":
            case "long":
            case "double":
            case "decimal":
                return paramValue.toString();
            case "boolean":
                return paramValue.toString().toLowerCase();
            default:
                return "'" + paramValue.toString() + "'";
        }
    }

    /**
     * 执行数据查询
     */
    private DataSourceClient.QueryResult executeQuery(String dataSourceId, String sql, Map<String, Object> params) {
        try {
            DataSourceClient.QueryRequest request = new DataSourceClient.QueryRequest();
        request.setSql(sql);
        request.setParameters(params);
        
        DataSourceClient.ApiResponse<DataSourceClient.QueryResult> response = 
            dataSourceClient.executeQuery(dataSourceId, request);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("数据查询失败: " + response.getMessage());
            }
            
            return response.getData();
            
        } catch (Exception e) {
            log.error("执行数据查询失败，数据源ID: {}, SQL: {}", dataSourceId, sql, e);
            throw new RuntimeException("数据查询失败: " + e.getMessage());
        }
    }

    /**
     * 记录接口执行日志
     */
    private void logInterfaceExecution(Interface interfaceInfo, Map<String, Object> requestParams, 
                                     InterfaceExecutionResult result, Exception exception) {
        try {
            // 这里应该记录到数据库或日志系统
            // 暂时只记录到应用日志
            if (exception == null) {
                log.info("接口执行成功 - 接口ID: {}, 接口名称: {}, 执行时间: {}, 返回记录数: {}", 
                    interfaceInfo.getId(), interfaceInfo.getInterfaceName(), 
                    result.getExecuteTime(), result.getTotalCount());
            } else {
                log.error("接口执行失败 - 接口ID: {}, 接口名称: {}, 执行时间: {}, 错误信息: {}", 
                    interfaceInfo.getId(), interfaceInfo.getInterfaceName(), 
                    result.getExecuteTime(), exception.getMessage());
            }
        } catch (Exception e) {
            log.warn("记录接口执行日志失败", e);
        }
    }

    /**
     * 接口执行结果
     */
    public static class InterfaceExecutionResult {
        private String interfaceId;
        private String interfaceName;
        private String executeTime;
        private boolean success;
        private String errorMessage;
        private List<Map<String, Object>> data;
        private long totalCount;
        private String executeSql;
        
        // getters and setters
        public String getInterfaceId() { return interfaceId; }
        public void setInterfaceId(String interfaceId) { this.interfaceId = interfaceId; }
        public String getInterfaceName() { return interfaceName; }
        public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }
        public String getExecuteTime() { return executeTime; }
        public void setExecuteTime(String executeTime) { this.executeTime = executeTime; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public List<Map<String, Object>> getData() { return data; }
        public void setData(List<Map<String, Object>> data) { this.data = data; }
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public String getExecuteSql() { return executeSql; }
        public void setExecuteSql(String executeSql) { this.executeSql = executeSql; }
    }
}