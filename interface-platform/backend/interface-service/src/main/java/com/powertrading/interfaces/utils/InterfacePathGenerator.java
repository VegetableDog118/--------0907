package com.powertrading.interfaces.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 接口路径生成工具类
 * 负责生成规范化的接口路径
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Component
public class InterfacePathGenerator {

    private static final Logger log = LoggerFactory.getLogger(InterfacePathGenerator.class);

    @Value("${interface.path.prefix:/api/data}")
    private String pathPrefix;

    @Value("${interface.path.version:v1}")
    private String pathVersion;

    // 分类路径映射
    private Map<String, String> categoryPathMapping;

    // 中文到英文的术语映射
    private Map<String, String> chineseToEnglishMapping;

    // 路径验证正则表达式
    private static final Pattern PATH_PATTERN = Pattern.compile("^[a-z0-9\\-_/]+$");
    private static final Pattern VALID_PATH_SEGMENT = Pattern.compile("^[a-z0-9][a-z0-9\\-_]*[a-z0-9]$|^[a-z0-9]$");

    @PostConstruct
    public void init() {
        initCategoryPathMapping();
        initChineseToEnglishMapping();
    }

    /**
     * 生成接口路径
     *
     * @param categoryCode 分类编码
     * @param interfaceName 接口名称
     * @param tableName 表名
     * @return 生成的接口路径
     */
    public String generateInterfacePath(String categoryCode, String interfaceName, String tableName) {
        try {
            // 获取分类路径
            String categoryPath = categoryPathMapping.getOrDefault(categoryCode, "general");
            
            // 转换接口名称为英文路径
            String interfacePathName = convertChineseNameToEnglishPath(interfaceName);
            
            // 转换表名为路径格式
            String tablePathName = convertTableNameToPath(tableName);
            
            // 组装完整路径
            String fullPath = String.format("%s/%s/%s/%s/%s", 
                pathPrefix, pathVersion, categoryPath, tablePathName, interfacePathName);
            
            // 规范化路径
            return normalizePath(fullPath);
            
        } catch (Exception e) {
            log.error("生成接口路径失败，分类: {}, 接口名称: {}, 表名: {}", 
                categoryCode, interfaceName, tableName, e);
            throw new RuntimeException("生成接口路径失败: " + e.getMessage());
        }
    }

    /**
     * 生成自定义接口路径
     *
     * @param request 路径生成请求
     * @return 生成的接口路径
     */
    public String generateCustomInterfacePath(PathGenerationRequest request) {
        try {
            List<String> pathSegments = new ArrayList<>();
            
            // 添加前缀
            if (StringUtils.hasText(request.getCustomPrefix())) {
                pathSegments.add(normalizePathSegment(request.getCustomPrefix()));
            } else {
                pathSegments.add(pathPrefix.substring(1)); // 移除开头的 /
            }
            
            // 添加版本
            if (StringUtils.hasText(request.getCustomVersion())) {
                pathSegments.add(normalizePathSegment(request.getCustomVersion()));
            } else {
                pathSegments.add(pathVersion);
            }
            
            // 添加分类路径
            String categoryPath = categoryPathMapping.getOrDefault(request.getCategoryCode(), "general");
            pathSegments.add(categoryPath);
            
            // 添加业务模块路径
            if (StringUtils.hasText(request.getBusinessModule())) {
                pathSegments.add(normalizePathSegment(request.getBusinessModule()));
            }
            
            // 添加资源路径
            String resourcePath = convertTableNameToPath(request.getTableName());
            pathSegments.add(resourcePath);
            
            // 添加操作路径
            String operationPath = convertChineseNameToEnglishPath(request.getInterfaceName());
            pathSegments.add(operationPath);
            
            // 组装完整路径
            String fullPath = "/" + String.join("/", pathSegments);
            
            // 验证路径长度
            if (fullPath.length() > 200) {
                throw new RuntimeException("生成的路径过长，超过200字符限制");
            }
            
            return normalizePath(fullPath);
            
        } catch (Exception e) {
            log.error("生成自定义接口路径失败", e);
            throw new RuntimeException("生成自定义接口路径失败: " + e.getMessage());
        }
    }

    /**
     * 验证接口路径
     *
     * @param path 接口路径
     * @return 验证结果
     */
    public PathValidationResult validateInterfacePath(String path) {
        PathValidationResult result = new PathValidationResult();
        result.setPath(path);
        result.setValid(true);
        result.setWarnings(new ArrayList<>());
        result.setErrors(new ArrayList<>());
        
        try {
            if (!StringUtils.hasText(path)) {
                result.setValid(false);
                result.getErrors().add("路径不能为空");
                return result;
            }
            
            // 检查路径格式
            if (!path.startsWith("/")) {
                result.getErrors().add("路径必须以 / 开头");
                result.setValid(false);
            }
            
            // 检查路径长度
            if (path.length() > 200) {
                result.getErrors().add("路径长度不能超过200字符");
                result.setValid(false);
            }
            
            // 检查路径字符
            if (!PATH_PATTERN.matcher(path).matches()) {
                result.getErrors().add("路径包含非法字符，只允许小写字母、数字、连字符、下划线和斜杠");
                result.setValid(false);
            }
            
            // 检查路径段
            String[] segments = path.split("/");
            for (int i = 1; i < segments.length; i++) { // 跳过第一个空段
                String segment = segments[i];
                if (!VALID_PATH_SEGMENT.matcher(segment).matches()) {
                    result.getWarnings().add("路径段 '" + segment + "' 不符合命名规范");
                }
            }
            
            // 检查路径深度
            if (segments.length > 8) {
                result.getWarnings().add("路径层级过深，建议不超过7层");
            }
            
            // 检查重复段
            Set<String> uniqueSegments = new HashSet<>();
            for (int i = 1; i < segments.length; i++) {
                if (!uniqueSegments.add(segments[i])) {
                    result.getWarnings().add("路径中存在重复段: " + segments[i]);
                }
            }
            
        } catch (Exception e) {
            result.setValid(false);
            result.getErrors().add("路径验证异常: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 生成路径建议
     *
     * @param interfaceName 接口名称
     * @param tableName 表名
     * @param categoryCode 分类编码
     * @return 路径建议列表
     */
    public List<String> generatePathSuggestions(String interfaceName, String tableName, String categoryCode) {
        List<String> suggestions = new ArrayList<>();
        
        try {
            // 标准路径
            String standardPath = generateInterfacePath(categoryCode, interfaceName, tableName);
            suggestions.add(standardPath);
            
            // 简化路径（移除版本）
            String simplifiedPath = standardPath.replace("/" + pathVersion, "");
            suggestions.add(simplifiedPath);
            
            // RESTful风格路径
            String restfulPath = generateRestfulPath(interfaceName, tableName, categoryCode);
            suggestions.add(restfulPath);
            
            // 业务导向路径
            String businessPath = generateBusinessOrientedPath(interfaceName, tableName, categoryCode);
            suggestions.add(businessPath);
            
            // 去重
            return suggestions.stream().distinct().collect(Collectors.toList());
            
        } catch (Exception e) {
            log.warn("生成路径建议失败", e);
            return List.of("/api/data/v1/general/unknown/query");
        }
    }

    /**
     * 生成RESTful风格路径
     */
    private String generateRestfulPath(String interfaceName, String tableName, String categoryCode) {
        String categoryPath = categoryPathMapping.getOrDefault(categoryCode, "general");
        String resourcePath = convertTableNameToPath(tableName);
        
        // 根据接口名称判断操作类型
        String operation = determineRestfulOperation(interfaceName);
        
        if ("list".equals(operation) || "query".equals(operation)) {
            return String.format("%s/%s/%s/%s", pathPrefix, pathVersion, categoryPath, resourcePath);
        } else {
            return String.format("%s/%s/%s/%s/%s", pathPrefix, pathVersion, categoryPath, resourcePath, operation);
        }
    }

    /**
     * 生成业务导向路径
     */
    private String generateBusinessOrientedPath(String interfaceName, String tableName, String categoryCode) {
        String categoryPath = categoryPathMapping.getOrDefault(categoryCode, "general");
        String businessAction = extractBusinessAction(interfaceName);
        String resourcePath = convertTableNameToPath(tableName);
        
        return String.format("%s/%s/%s/%s/%s", pathPrefix, pathVersion, businessAction, categoryPath, resourcePath);
    }

    /**
     * 确定RESTful操作类型
     */
    private String determineRestfulOperation(String interfaceName) {
        String lowerName = interfaceName.toLowerCase();
        
        if (lowerName.contains("查询") || lowerName.contains("获取") || lowerName.contains("列表")) {
            return "query";
        } else if (lowerName.contains("统计") || lowerName.contains("汇总")) {
            return "statistics";
        } else if (lowerName.contains("详情") || lowerName.contains("信息")) {
            return "detail";
        } else if (lowerName.contains("分析") || lowerName.contains("报表")) {
            return "analysis";
        }
        
        return "query";
    }

    /**
     * 提取业务动作
     */
    private String extractBusinessAction(String interfaceName) {
        String lowerName = interfaceName.toLowerCase();
        
        if (lowerName.contains("交易")) {
            return "trading";
        } else if (lowerName.contains("风险")) {
            return "risk";
        } else if (lowerName.contains("合规")) {
            return "compliance";
        } else if (lowerName.contains("市场")) {
            return "market";
        } else if (lowerName.contains("客户")) {
            return "customer";
        }
        
        return "business";
    }

    /**
     * 转换中文名称为英文路径
     *
     * @param chineseName 中文名称
     * @return 英文路径
     */
    private String convertChineseNameToEnglishPath(String chineseName) {
        if (!StringUtils.hasText(chineseName)) {
            return "unknown";
        }
        
        String result = chineseName.toLowerCase();
        
        // 替换中文术语
        for (Map.Entry<String, String> entry : chineseToEnglishMapping.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        
        // 移除非法字符，只保留字母、数字、连字符和下划线
        result = result.replaceAll("[^a-z0-9\\-_]", "-");
        
        // 移除多余的连字符
        result = result.replaceAll("-+", "-");
        
        // 移除首尾连字符
        result = result.replaceAll("^-+|-+$", "");
        
        return StringUtils.hasText(result) ? result : "unknown";
    }

    /**
     * 转换表名为路径格式
     *
     * @param tableName 表名
     * @return 路径格式的表名
     */
    private String convertTableNameToPath(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            return "unknown";
        }
        
        // 转换为小写
        String result = tableName.toLowerCase();
        
        // 移除表前缀（如 t_, tb_, table_ 等）
        result = result.replaceAll("^(t_|tb_|table_)", "");
        
        // 将下划线转换为连字符
        result = result.replace("_", "-");
        
        return result;
    }

    /**
     * 规范化路径段
     *
     * @param segment 路径段
     * @return 规范化后的路径段
     */
    private String normalizePathSegment(String segment) {
        if (!StringUtils.hasText(segment)) {
            return "unknown";
        }
        
        String result = segment.toLowerCase();
        result = result.replaceAll("[^a-z0-9\\-_]", "-");
        result = result.replaceAll("-+", "-");
        result = result.replaceAll("^-+|-+$", "");
        
        return StringUtils.hasText(result) ? result : "unknown";
    }

    /**
     * 规范化路径
     *
     * @param path 原始路径
     * @return 规范化后的路径
     */
    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        
        // 确保以 / 开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        // 移除重复的斜杠
        path = path.replaceAll("/+", "/");
        
        // 移除末尾的斜杠（除非是根路径）
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        return path;
    }

    /**
     * 初始化分类路径映射
     */
    private void initCategoryPathMapping() {
        categoryPathMapping = new HashMap<>();
        categoryPathMapping.put("BASIC_DATA", "basic");
        categoryPathMapping.put("BUSINESS_DATA", "business");
        categoryPathMapping.put("STATISTICAL_DATA", "statistics");
        categoryPathMapping.put("REFERENCE_DATA", "reference");
        categoryPathMapping.put("TRANSACTION_DATA", "transaction");
        categoryPathMapping.put("MARKET_DATA", "market");
        categoryPathMapping.put("RISK_DATA", "risk");
        categoryPathMapping.put("COMPLIANCE_DATA", "compliance");
    }

    /**
     * 初始化中文到英文的术语映射
     */
    private void initChineseToEnglishMapping() {
        chineseToEnglishMapping = new HashMap<>();
        
        // 基础术语
        chineseToEnglishMapping.put("查询", "query");
        chineseToEnglishMapping.put("获取", "get");
        chineseToEnglishMapping.put("列表", "list");
        chineseToEnglishMapping.put("详情", "detail");
        chineseToEnglishMapping.put("信息", "info");
        chineseToEnglishMapping.put("数据", "data");
        chineseToEnglishMapping.put("统计", "statistics");
        chineseToEnglishMapping.put("汇总", "summary");
        chineseToEnglishMapping.put("报表", "report");
        chineseToEnglishMapping.put("分析", "analysis");
        
        // 业务术语
        chineseToEnglishMapping.put("交易", "trade");
        chineseToEnglishMapping.put("订单", "order");
        chineseToEnglishMapping.put("客户", "customer");
        chineseToEnglishMapping.put("用户", "user");
        chineseToEnglishMapping.put("账户", "account");
        chineseToEnglishMapping.put("资金", "fund");
        chineseToEnglishMapping.put("持仓", "position");
        chineseToEnglishMapping.put("市场", "market");
        chineseToEnglishMapping.put("价格", "price");
        chineseToEnglishMapping.put("风险", "risk");
        chineseToEnglishMapping.put("合规", "compliance");
        chineseToEnglishMapping.put("监管", "regulation");
        
        // 时间术语
        chineseToEnglishMapping.put("日", "daily");
        chineseToEnglishMapping.put("周", "weekly");
        chineseToEnglishMapping.put("月", "monthly");
        chineseToEnglishMapping.put("年", "yearly");
        chineseToEnglishMapping.put("实时", "realtime");
        chineseToEnglishMapping.put("历史", "history");
        
        // 操作术语
        chineseToEnglishMapping.put("创建", "create");
        chineseToEnglishMapping.put("更新", "update");
        chineseToEnglishMapping.put("删除", "delete");
        chineseToEnglishMapping.put("修改", "modify");
        chineseToEnglishMapping.put("新增", "add");
        chineseToEnglishMapping.put("移除", "remove");
    }

    // 内部类定义
    
    public static class PathGenerationRequest {
        private String categoryCode;
        private String interfaceName;
        private String tableName;
        private String businessModule;
        private String customPrefix;
        private String customVersion;
        
        // getters and setters
        public String getCategoryCode() { return categoryCode; }
        public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
        public String getInterfaceName() { return interfaceName; }
        public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getBusinessModule() { return businessModule; }
        public void setBusinessModule(String businessModule) { this.businessModule = businessModule; }
        public String getCustomPrefix() { return customPrefix; }
        public void setCustomPrefix(String customPrefix) { this.customPrefix = customPrefix; }
        public String getCustomVersion() { return customVersion; }
        public void setCustomVersion(String customVersion) { this.customVersion = customVersion; }
    }

    public static class PathValidationResult {
        private String path;
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        
        // getters and setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }
}