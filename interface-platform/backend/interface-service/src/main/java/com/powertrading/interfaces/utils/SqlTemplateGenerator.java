package com.powertrading.interfaces.utils;

import com.powertrading.interfaces.dto.InterfaceGenerationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * SQL模板生成器
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Component
public class SqlTemplateGenerator {

    // 聚合函数映射
    private static final Map<String, String> AGGREGATE_FUNCTIONS = new HashMap<>();
    static {
        AGGREGATE_FUNCTIONS.put("count", "COUNT(*)");
        AGGREGATE_FUNCTIONS.put("sum", "SUM({})");
        AGGREGATE_FUNCTIONS.put("avg", "AVG({})");
        AGGREGATE_FUNCTIONS.put("max", "MAX({})");
        AGGREGATE_FUNCTIONS.put("min", "MIN({})");
    }

    // 数据类型映射
    private static final Map<String, String> TYPE_MAPPING = new HashMap<>();
    static {
        TYPE_MAPPING.put("string", "VARCHAR");
        TYPE_MAPPING.put("integer", "INT");
        TYPE_MAPPING.put("number", "DECIMAL");
        TYPE_MAPPING.put("date", "DATE");
        TYPE_MAPPING.put("datetime", "DATETIME");
        TYPE_MAPPING.put("boolean", "BOOLEAN");
    }

    /**
     * 生成SQL模板
     *
     * @param tableName 表名
     * @param parameters 参数列表
     * @return SQL模板
     */
    public String generateSqlTemplate(String tableName, 
                                    List<InterfaceGenerationRequest.ParameterConfiguration> parameters) {
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName);
        
        // 构建WHERE条件
        List<String> whereConditions = buildWhereConditions(parameters);
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ");
            sql.append(String.join(" AND ", whereConditions));
        }
        
        // 添加排序（如果有时间字段）
        String orderByClause = buildOrderByClause(tableName);
        if (StringUtils.hasText(orderByClause)) {
            sql.append(" ").append(orderByClause);
        }
        
        // 添加限制（防止返回过多数据）
        sql.append(" LIMIT 1000");
        
        return sql.toString();
    }

    /**
     * 构建WHERE条件
     *
     * @param parameters 参数列表
     * @return WHERE条件列表
     */
    private List<String> buildWhereConditions(List<InterfaceGenerationRequest.ParameterConfiguration> parameters) {
        return parameters.stream()
            .filter(param -> !"appId".equals(param.getParamName())) // appId不作为查询条件
            .filter(param -> param.getRequired() != null && param.getRequired()) // 只处理必需参数
            .map(this::buildCondition)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
    }

    /**
     * 构建单个条件
     *
     * @param param 参数配置
     * @return SQL条件
     */
    private String buildCondition(InterfaceGenerationRequest.ParameterConfiguration param) {
        String paramName = param.getParamName();
        String paramType = param.getParamType();
        
        switch (paramName) {
            case "dataTime":
                return buildDateTimeCondition(paramName, paramType);
            case "startTime":
            case "beginTime":
                return buildDateTimeCondition(paramName, paramType);
            case "endTime":
            case "finishTime":
                return buildDateTimeCondition(paramName, paramType);
            default:
                return buildGeneralCondition(paramName, paramType);
        }
    }

    /**
     * 构建日期时间条件
     *
     * @param paramName 参数名
     * @param paramType 参数类型
     * @return SQL条件
     */
    private String buildDateTimeCondition(String paramName, String paramType) {
        if ("date".equals(paramType)) {
            // 日期类型：精确匹配或范围查询
            if (paramName.contains("start") || paramName.contains("begin")) {
                return "DATE(data_time) >= #{" + paramName + "}";
            } else if (paramName.contains("end") || paramName.contains("finish")) {
                return "DATE(data_time) <= #{" + paramName + "}";
            } else {
                return "DATE(data_time) = #{" + paramName + "}";
            }
        } else if ("datetime".equals(paramType)) {
            // 日期时间类型
            if (paramName.contains("start") || paramName.contains("begin")) {
                return "data_time >= #{" + paramName + "}";
            } else if (paramName.contains("end") || paramName.contains("finish")) {
                return "data_time <= #{" + paramName + "}";
            } else {
                return "DATE(data_time) = DATE(#{" + paramName + "})";
            }
        } else {
            // 字符串类型的日期
            return "DATE(data_time) = #{" + paramName + "}";
        }
    }

    /**
     * 构建通用条件
     *
     * @param paramName 参数名
     * @param paramType 参数类型
     * @return SQL条件
     */
    private String buildGeneralCondition(String paramName, String paramType) {
        // 根据参数名推断数据库字段名
        String columnName = convertParamNameToColumnName(paramName);
        
        switch (paramType) {
            case "string":
                // 字符串类型：支持模糊查询
                if (paramName.contains("name") || paramName.contains("Name")) {
                    return columnName + " LIKE CONCAT('%', #{" + paramName + "}, '%')";
                } else {
                    return columnName + " = #{" + paramName + "}";
                }
            case "integer":
            case "number":
                return columnName + " = #{" + paramName + "}";
            case "boolean":
                return columnName + " = #{" + paramName + "}";
            default:
                return columnName + " = #{" + paramName + "}";
        }
    }

    /**
     * 将参数名转换为数据库字段名
     *
     * @param paramName 参数名
     * @return 数据库字段名
     */
    private String convertParamNameToColumnName(String paramName) {
        // 驼峰转下划线
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < paramName.length(); i++) {
            char ch = paramName.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * 构建ORDER BY子句
     *
     * @param tableName 表名
     * @return ORDER BY子句
     */
    private String buildOrderByClause(String tableName) {
        // 根据表名推断排序字段
        if (tableName.contains("time") || tableName.contains("date")) {
            return "ORDER BY data_time DESC";
        } else if (tableName.contains("log") || tableName.contains("record")) {
            return "ORDER BY create_time DESC";
        } else if (tableName.contains("plan") || tableName.contains("schedule")) {
            return "ORDER BY plan_date DESC, create_time DESC";
        } else {
            // 默认按ID排序
            return "ORDER BY id DESC";
        }
    }

    /**
     * 生成分页SQL模板
     *
     * @param tableName 表名
     * @param parameters 参数列表
     * @return 分页SQL模板
     */
    public String generatePagedSqlTemplate(String tableName, 
                                          List<InterfaceGenerationRequest.ParameterConfiguration> parameters) {
        
        StringBuilder sql = new StringBuilder();
        
        // 构建基础查询
        sql.append("SELECT * FROM ").append(tableName);
        
        // 构建WHERE条件
        List<String> whereConditions = buildWhereConditions(parameters);
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ");
            sql.append(String.join(" AND ", whereConditions));
        }
        
        // 添加排序
        String orderByClause = buildOrderByClause(tableName);
        if (StringUtils.hasText(orderByClause)) {
            sql.append(" ").append(orderByClause);
        }
        
        // 添加分页
        sql.append(" LIMIT #{offset}, #{limit}");
        
        return sql.toString();
    }

    /**
     * 生成统计SQL模板
     *
     * @param tableName 表名
     * @param parameters 参数列表
     * @return 统计SQL模板
     */
    public String generateCountSqlTemplate(String tableName, 
                                         List<InterfaceGenerationRequest.ParameterConfiguration> parameters) {
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) as total FROM ").append(tableName);
        
        // 构建WHERE条件
        List<String> whereConditions = buildWhereConditions(parameters);
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ");
            sql.append(String.join(" AND ", whereConditions));
        }
        
        return sql.toString();
    }

    /**
     * 验证SQL模板语法
     *
     * @param sqlTemplate SQL模板
     * @return 验证结果
     */
    public SqlValidationResult validateSqlTemplate(String sqlTemplate) {
        SqlValidationResult result = new SqlValidationResult();
        result.setValid(true);
        
        try {
            // 基本语法检查
            if (!StringUtils.hasText(sqlTemplate)) {
                result.setValid(false);
                result.setErrorMessage("SQL模板不能为空");
                return result;
            }
            
            // 检查是否包含危险操作
            String upperSql = sqlTemplate.toUpperCase();
            if (upperSql.contains("DROP") || upperSql.contains("DELETE") || 
                upperSql.contains("UPDATE") || upperSql.contains("INSERT") ||
                upperSql.contains("TRUNCATE") || upperSql.contains("ALTER")) {
                result.setValid(false);
                result.setErrorMessage("SQL模板不能包含修改数据的操作");
                return result;
            }
            
            // 检查是否以SELECT开头
            if (!upperSql.trim().startsWith("SELECT")) {
                result.setValid(false);
                result.setErrorMessage("SQL模板必须以SELECT开头");
                return result;
            }
            
        } catch (Exception e) {
            result.setValid(false);
            result.setErrorMessage("SQL模板验证失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 生成高级SQL模板
     *
     * @param request SQL生成请求
     * @return 高级SQL模板
     */
    public String generateAdvancedSqlTemplate(SqlGenerationRequest request) {
        StringBuilder sql = new StringBuilder();
        
        // 构建SELECT子句
        if (request.getSelectFields() != null && !request.getSelectFields().isEmpty()) {
            sql.append("SELECT ");
            if (request.isDistinct()) {
                sql.append("DISTINCT ");
            }
            sql.append(String.join(", ", request.getSelectFields()));
        } else {
            sql.append("SELECT *");
        }
        
        // 构建FROM子句
        sql.append(" FROM ").append(request.getTableName());
        
        // 构建JOIN子句
        if (request.getJoins() != null && !request.getJoins().isEmpty()) {
            for (String join : request.getJoins()) {
                sql.append(" ").append(join);
            }
        }
        
        // 构建WHERE子句
        if (request.getWhereConditions() != null && !request.getWhereConditions().isEmpty()) {
            sql.append(" WHERE ");
            sql.append(String.join(" AND ", request.getWhereConditions()));
        }
        
        // 构建GROUP BY子句
        if (request.getGroupByFields() != null && !request.getGroupByFields().isEmpty()) {
            sql.append(" GROUP BY ");
            sql.append(String.join(", ", request.getGroupByFields()));
        }
        
        // 构建HAVING子句
        if (StringUtils.hasText(request.getHavingCondition())) {
            sql.append(" HAVING ").append(request.getHavingCondition());
        }
        
        // 构建ORDER BY子句
        if (request.getOrderByFields() != null && !request.getOrderByFields().isEmpty()) {
            sql.append(" ORDER BY ");
            sql.append(String.join(", ", request.getOrderByFields()));
        }
        
        // 构建LIMIT子句
        if (request.getLimit() > 0) {
            sql.append(" LIMIT ").append(request.getLimit());
            if (request.getOffset() > 0) {
                sql.append(" OFFSET ").append(request.getOffset());
            }
        }
        
        return sql.toString();
    }

    /**
     * 生成统计分析SQL模板
     *
     * @param tableName 表名
     * @param analysisType 分析类型
     * @param groupByField 分组字段
     * @param aggregateField 聚合字段
     * @return 统计分析SQL模板
     */
    public String generateAnalyticsSqlTemplate(String tableName, String analysisType, 
                                              String groupByField, String aggregateField) {
        StringBuilder sql = new StringBuilder();
        
        switch (analysisType.toLowerCase()) {
            case "summary":
                // 汇总统计
                sql.append("SELECT ")
                   .append("COUNT(*) as total_count, ")
                   .append("COUNT(DISTINCT ").append(groupByField).append(") as unique_count")
                   .append(" FROM ").append(tableName);
                break;
                
            case "groupby":
                // 分组统计
                sql.append("SELECT ")
                   .append(groupByField).append(", ")
                   .append("COUNT(*) as count")
                   .append(" FROM ").append(tableName)
                   .append(" GROUP BY ").append(groupByField)
                   .append(" ORDER BY count DESC");
                break;
                
            case "trend":
                // 趋势分析
                sql.append("SELECT ")
                   .append("DATE(").append(groupByField).append(") as date, ")
                   .append("COUNT(*) as count")
                   .append(" FROM ").append(tableName)
                   .append(" GROUP BY DATE(").append(groupByField).append(")")
                   .append(" ORDER BY date");
                break;
                
            case "aggregate":
                // 聚合分析
                sql.append("SELECT ")
                   .append("SUM(").append(aggregateField).append(") as total, ")
                   .append("AVG(").append(aggregateField).append(") as average, ")
                   .append("MAX(").append(aggregateField).append(") as maximum, ")
                   .append("MIN(").append(aggregateField).append(") as minimum")
                   .append(" FROM ").append(tableName);
                break;
                
            default:
                // 默认统计
                sql.append("SELECT COUNT(*) as total FROM ").append(tableName);
        }
        
        return sql.toString();
    }

    /**
     * 优化SQL模板
     *
     * @param originalSql 原始SQL
     * @param options 优化选项
     * @return 优化后的SQL
     */
    public String optimizeSqlTemplate(String originalSql, SqlOptimizationOptions options) {
        String optimizedSql = originalSql;
        
        if (options.isAddIndexHints()) {
            // 添加索引提示
            optimizedSql = addIndexHints(optimizedSql);
        }
        
        if (options.isOptimizeJoins()) {
            // 优化JOIN操作
            optimizedSql = optimizeJoins(optimizedSql);
        }
        
        if (options.isAddLimitClause() && !optimizedSql.toUpperCase().contains("LIMIT")) {
            // 添加LIMIT子句防止返回过多数据
            optimizedSql += " LIMIT 1000";
        }
        
        if (options.isFormatSql()) {
            // 格式化SQL
            optimizedSql = formatSql(optimizedSql);
        }
        
        return optimizedSql;
    }

    /**
     * 添加索引提示
     */
    private String addIndexHints(String sql) {
        // 简单的索引提示添加逻辑
        if (sql.contains("data_time")) {
            sql = sql.replace("FROM ", "FROM /*+ USE INDEX(idx_data_time) */ ");
        }
        return sql;
    }

    /**
     * 优化JOIN操作
     */
    private String optimizeJoins(String sql) {
        // 简单的JOIN优化逻辑
        return sql.replace("JOIN", "INNER JOIN");
    }

    /**
     * 格式化SQL
     */
    private String formatSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    /**
     * SQL生成请求
     */
    public static class SqlGenerationRequest {
        private String tableName;
        private List<String> selectFields;
        private boolean distinct;
        private List<String> joins;
        private List<String> whereConditions;
        private List<String> groupByFields;
        private String havingCondition;
        private List<String> orderByFields;
        private int limit;
        private int offset;
        
        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public List<String> getSelectFields() { return selectFields; }
        public void setSelectFields(List<String> selectFields) { this.selectFields = selectFields; }
        public boolean isDistinct() { return distinct; }
        public void setDistinct(boolean distinct) { this.distinct = distinct; }
        public List<String> getJoins() { return joins; }
        public void setJoins(List<String> joins) { this.joins = joins; }
        public List<String> getWhereConditions() { return whereConditions; }
        public void setWhereConditions(List<String> whereConditions) { this.whereConditions = whereConditions; }
        public List<String> getGroupByFields() { return groupByFields; }
        public void setGroupByFields(List<String> groupByFields) { this.groupByFields = groupByFields; }
        public String getHavingCondition() { return havingCondition; }
        public void setHavingCondition(String havingCondition) { this.havingCondition = havingCondition; }
        public List<String> getOrderByFields() { return orderByFields; }
        public void setOrderByFields(List<String> orderByFields) { this.orderByFields = orderByFields; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public int getOffset() { return offset; }
        public void setOffset(int offset) { this.offset = offset; }
    }

    /**
     * SQL优化选项
     */
    public static class SqlOptimizationOptions {
        private boolean addIndexHints = false;
        private boolean optimizeJoins = false;
        private boolean addLimitClause = true;
        private boolean formatSql = true;
        
        // Getters and Setters
        public boolean isAddIndexHints() { return addIndexHints; }
        public void setAddIndexHints(boolean addIndexHints) { this.addIndexHints = addIndexHints; }
        public boolean isOptimizeJoins() { return optimizeJoins; }
        public void setOptimizeJoins(boolean optimizeJoins) { this.optimizeJoins = optimizeJoins; }
        public boolean isAddLimitClause() { return addLimitClause; }
        public void setAddLimitClause(boolean addLimitClause) { this.addLimitClause = addLimitClause; }
        public boolean isFormatSql() { return formatSql; }
        public void setFormatSql(boolean formatSql) { this.formatSql = formatSql; }
    }

    /**
     * SQL验证结果
     */
    public static class SqlValidationResult {
        private boolean valid;
        private String errorMessage;
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}