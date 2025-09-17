package com.powertrading.auth.service;

import com.powertrading.auth.dto.ApiKeyValidateRequest;
import com.powertrading.auth.dto.ApiKeyValidateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * API密钥管理服务
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class ApiKeyService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${auth.api-key.length:32}")
    private Integer apiKeyLength;

    @Value("${auth.api-key.secret-length:64}")
    private Integer secretKeyLength;

    @Value("${auth.api-key.default-expiration:31536000}")
    private Long defaultExpiration; // 1年

    private static final String API_KEY_PREFIX = "auth:apikey:";
    private static final String APP_ID_PREFIX = "auth:appid:";
    private static final String USER_API_KEYS_PREFIX = "auth:user:apikeys:";
    private static final String API_CALL_COUNT_PREFIX = "auth:apicall:count:";
    private static final String API_CALL_LIMIT_PREFIX = "auth:apicall:limit:";
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成API密钥
     */
    public Map<String, String> generateApiKey(String userId, String username, String companyName,
                                            List<String> permissions, List<String> allowedInterfaces,
                                            Long dailyLimit) {
        try {
            // 生成AppId和API密钥
            String appId = generateAppId();
            String apiKey = generateRandomKey(apiKeyLength);
            String secretKey = generateRandomKey(secretKeyLength);

            // 构建API密钥信息
            Map<String, Object> apiKeyInfo = new HashMap<>();
            apiKeyInfo.put("userId", userId);
            apiKeyInfo.put("username", username);
            apiKeyInfo.put("companyName", companyName != null ? companyName : "");
            apiKeyInfo.put("appId", appId);
            apiKeyInfo.put("apiKey", apiKey);
            apiKeyInfo.put("secretKey", secretKey);
            apiKeyInfo.put("permissions", permissions != null ? permissions : new ArrayList<>());
            apiKeyInfo.put("allowedInterfaces", allowedInterfaces != null ? allowedInterfaces : new ArrayList<>());
            apiKeyInfo.put("dailyLimit", dailyLimit != null ? dailyLimit : 10000L);
            apiKeyInfo.put("status", "active");
            apiKeyInfo.put("createTime", System.currentTimeMillis());
            apiKeyInfo.put("expireTime", System.currentTimeMillis() + (defaultExpiration * 1000));
            apiKeyInfo.put("lastUsedTime", 0L);
            apiKeyInfo.put("totalCalls", 0L);

            // 缓存API密钥信息
            String apiKeyRedisKey = API_KEY_PREFIX + apiKey;
            String appIdRedisKey = APP_ID_PREFIX + appId;
            
            redisTemplate.opsForHash().putAll(apiKeyRedisKey, apiKeyInfo);
            redisTemplate.opsForHash().putAll(appIdRedisKey, apiKeyInfo);
            
            // 设置过期时间
            redisTemplate.expire(apiKeyRedisKey, defaultExpiration, TimeUnit.SECONDS);
            redisTemplate.expire(appIdRedisKey, defaultExpiration, TimeUnit.SECONDS);

            // 添加到用户API密钥集合
            String userApiKeysKey = USER_API_KEYS_PREFIX + userId;
            redisTemplate.opsForSet().add(userApiKeysKey, apiKey);
            redisTemplate.expire(userApiKeysKey, defaultExpiration, TimeUnit.SECONDS);

            // 初始化调用计数
            initializeDailyCallCount(apiKey);

            log.info("API密钥生成成功: userId={}, appId={}", userId, appId);

            Map<String, String> result = new HashMap<>();
            result.put("appId", appId);
            result.put("apiKey", apiKey);
            result.put("secretKey", secretKey);
            
            return result;
        } catch (Exception e) {
            log.error("生成API密钥失败: userId={}", userId, e);
            throw new RuntimeException("生成API密钥失败: " + e.getMessage());
        }
    }

    /**
     * 验证API密钥
     */
    public ApiKeyValidateResponse validateApiKey(ApiKeyValidateRequest request) {
        try {
            String appId = request.getAppId();
            String apiKey = request.getApiKey();
            
            // 检查必要参数
            if (!StringUtils.hasText(appId)) {
                return ApiKeyValidateResponse.failure("INVALID_APP_ID", "AppId不能为空");
            }

            // 获取API密钥信息
            Map<Object, Object> apiKeyInfo = getApiKeyInfo(appId);
            if (apiKeyInfo == null || apiKeyInfo.isEmpty()) {
                return ApiKeyValidateResponse.failure("APP_ID_NOT_FOUND", "AppId不存在");
            }

            // 检查状态
            String status = (String) apiKeyInfo.get("status");
            if (!"active".equals(status)) {
                return ApiKeyValidateResponse.failure("API_KEY_DISABLED", "API密钥已禁用");
            }

            // 检查过期时间
            Long expireTime = (Long) apiKeyInfo.get("expireTime");
            if (expireTime != null && expireTime < System.currentTimeMillis()) {
                return ApiKeyValidateResponse.failure("API_KEY_EXPIRED", "API密钥已过期");
            }

            // 验证签名（如果提供）
            if (StringUtils.hasText(request.getSignature())) {
                if (!validateSignature(request, (String) apiKeyInfo.get("secretKey"))) {
                    return ApiKeyValidateResponse.failure("INVALID_SIGNATURE", "签名验证失败");
                }
            }

            // 检查调用限制
            Long dailyLimit = (Long) apiKeyInfo.get("dailyLimit");
            Long remainingCalls = checkDailyLimit(apiKey, dailyLimit);
            if (remainingCalls <= 0) {
                return ApiKeyValidateResponse.failure("DAILY_LIMIT_EXCEEDED", "今日调用次数已达上限");
            }

            // 检查接口权限
            @SuppressWarnings("unchecked")
            List<String> allowedInterfaces = (List<String>) apiKeyInfo.get("allowedInterfaces");
            if (StringUtils.hasText(request.getRequestPath()) && 
                allowedInterfaces != null && !allowedInterfaces.isEmpty()) {
                
                boolean hasPermission = allowedInterfaces.stream()
                        .anyMatch(pattern -> matchesPattern(request.getRequestPath(), pattern));
                
                if (!hasPermission) {
                    return ApiKeyValidateResponse.failure("INTERFACE_NOT_ALLOWED", "无权限访问该接口");
                }
            }

            // 更新使用统计
            updateUsageStatistics(apiKey, apiKeyInfo);

            // 构建成功响应
            String userId = (String) apiKeyInfo.get("userId");
            String username = (String) apiKeyInfo.get("username");
            String companyName = (String) apiKeyInfo.get("companyName");
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) apiKeyInfo.get("permissions");
            
            LocalDateTime expireDateTime = expireTime != null ? 
                    LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(expireTime), ZoneId.systemDefault()) : null;

            log.debug("API密钥验证成功: appId={}, userId={}", appId, userId);

            return ApiKeyValidateResponse.success(
                    userId, appId, username, companyName, permissions,
                    allowedInterfaces, expireDateTime, remainingCalls, dailyLimit
            );
        } catch (Exception e) {
            log.error("API密钥验证失败: appId={}", request.getAppId(), e);
            return ApiKeyValidateResponse.failure("VALIDATION_ERROR", "验证过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 禁用API密钥
     */
    public void disableApiKey(String apiKey) {
        try {
            String apiKeyRedisKey = API_KEY_PREFIX + apiKey;
            Map<Object, Object> apiKeyInfo = redisTemplate.opsForHash().entries(apiKeyRedisKey);
            
            if (!apiKeyInfo.isEmpty()) {
                String appId = (String) apiKeyInfo.get("appId");
                
                // 更新状态
                redisTemplate.opsForHash().put(apiKeyRedisKey, "status", "disabled");
                redisTemplate.opsForHash().put(APP_ID_PREFIX + appId, "status", "disabled");
                
                log.info("API密钥已禁用: apiKey={}, appId={}", apiKey, appId);
            }
        } catch (Exception e) {
            log.error("禁用API密钥失败: apiKey={}", apiKey, e);
            throw new RuntimeException("禁用API密钥失败: " + e.getMessage());
        }
    }

    /**
     * 启用API密钥
     */
    public void enableApiKey(String apiKey) {
        try {
            String apiKeyRedisKey = API_KEY_PREFIX + apiKey;
            Map<Object, Object> apiKeyInfo = redisTemplate.opsForHash().entries(apiKeyRedisKey);
            
            if (!apiKeyInfo.isEmpty()) {
                String appId = (String) apiKeyInfo.get("appId");
                
                // 更新状态
                redisTemplate.opsForHash().put(apiKeyRedisKey, "status", "active");
                redisTemplate.opsForHash().put(APP_ID_PREFIX + appId, "status", "active");
                
                log.info("API密钥已启用: apiKey={}, appId={}", apiKey, appId);
            }
        } catch (Exception e) {
            log.error("启用API密钥失败: apiKey={}", apiKey, e);
            throw new RuntimeException("启用API密钥失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的所有API密钥
     */
    public List<Map<String, Object>> getUserApiKeys(String userId) {
        try {
            String userApiKeysKey = USER_API_KEYS_PREFIX + userId;
            Set<Object> apiKeys = redisTemplate.opsForSet().members(userApiKeysKey);
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            if (apiKeys != null) {
                for (Object apiKeyObj : apiKeys) {
                    String apiKey = (String) apiKeyObj;
                    Map<Object, Object> apiKeyInfo = redisTemplate.opsForHash().entries(API_KEY_PREFIX + apiKey);
                    
                    if (!apiKeyInfo.isEmpty()) {
                        Map<String, Object> info = new HashMap<>();
                        info.put("appId", apiKeyInfo.get("appId"));
                        info.put("apiKey", maskApiKey(apiKey));
                        info.put("status", apiKeyInfo.get("status"));
                        info.put("createTime", apiKeyInfo.get("createTime"));
                        info.put("expireTime", apiKeyInfo.get("expireTime"));
                        info.put("lastUsedTime", apiKeyInfo.get("lastUsedTime"));
                        info.put("totalCalls", apiKeyInfo.get("totalCalls"));
                        info.put("dailyLimit", apiKeyInfo.get("dailyLimit"));
                        
                        result.add(info);
                    }
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取用户API密钥失败: userId={}", userId, e);
            throw new RuntimeException("获取用户API密钥失败: " + e.getMessage());
        }
    }

    /**
     * 生成AppId
     */
    private String generateAppId() {
        return "app_" + generateRandomKey(16);
    }

    /**
     * 生成随机密钥
     */
    private String generateRandomKey(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        
        return sb.toString();
    }

    /**
     * 获取API密钥信息
     */
    private Map<Object, Object> getApiKeyInfo(String appId) {
        String appIdRedisKey = APP_ID_PREFIX + appId;
        return redisTemplate.opsForHash().entries(appIdRedisKey);
    }

    /**
     * 验证签名
     */
    private boolean validateSignature(ApiKeyValidateRequest request, String secretKey) {
        try {
            // 构建签名字符串
            StringBuilder signString = new StringBuilder();
            signString.append(request.getAppId());
            
            if (StringUtils.hasText(request.getApiKey())) {
                signString.append(request.getApiKey());
            }
            
            if (request.getTimestamp() != null) {
                signString.append(request.getTimestamp());
            }
            
            if (StringUtils.hasText(request.getNonce())) {
                signString.append(request.getNonce());
            }
            
            if (StringUtils.hasText(request.getRequestMethod())) {
                signString.append(request.getRequestMethod());
            }
            
            if (StringUtils.hasText(request.getRequestPath())) {
                signString.append(request.getRequestPath());
            }

            // 计算HMAC-SHA256签名
            String calculatedSignature = calculateHmacSha256(signString.toString(), secretKey);
            
            return calculatedSignature.equals(request.getSignature());
        } catch (Exception e) {
            log.error("签名验证失败", e);
            return false;
        }
    }

    /**
     * 计算HMAC-SHA256签名
     */
    private String calculateHmacSha256(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 检查每日调用限制
     */
    private Long checkDailyLimit(String apiKey, Long dailyLimit) {
        try {
            String today = java.time.LocalDate.now().toString();
            String countKey = API_CALL_COUNT_PREFIX + apiKey + ":" + today;
            
            String countStr = (String) redisTemplate.opsForValue().get(countKey);
            long currentCount = countStr != null ? Long.parseLong(countStr) : 0L;
            
            return Math.max(0, dailyLimit - currentCount);
        } catch (Exception e) {
            log.error("检查每日调用限制失败", e);
            return 0L;
        }
    }

    /**
     * 初始化每日调用计数
     */
    private void initializeDailyCallCount(String apiKey) {
        try {
            String today = java.time.LocalDate.now().toString();
            String countKey = API_CALL_COUNT_PREFIX + apiKey + ":" + today;
            
            redisTemplate.opsForValue().setIfAbsent(countKey, "0");
            redisTemplate.expire(countKey, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("初始化每日调用计数失败", e);
        }
    }

    /**
     * 更新使用统计
     */
    private void updateUsageStatistics(String apiKey, Map<Object, Object> apiKeyInfo) {
        try {
            // 更新最后使用时间
            String apiKeyRedisKey = API_KEY_PREFIX + apiKey;
            String appId = (String) apiKeyInfo.get("appId");
            String appIdRedisKey = APP_ID_PREFIX + appId;
            
            long currentTime = System.currentTimeMillis();
            redisTemplate.opsForHash().put(apiKeyRedisKey, "lastUsedTime", currentTime);
            redisTemplate.opsForHash().put(appIdRedisKey, "lastUsedTime", currentTime);
            
            // 增加总调用次数
            redisTemplate.opsForHash().increment(apiKeyRedisKey, "totalCalls", 1);
            redisTemplate.opsForHash().increment(appIdRedisKey, "totalCalls", 1);
            
            // 增加今日调用次数
            String today = java.time.LocalDate.now().toString();
            String countKey = API_CALL_COUNT_PREFIX + apiKey + ":" + today;
            redisTemplate.opsForValue().increment(countKey);
            redisTemplate.expire(countKey, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("更新使用统计失败", e);
        }
    }

    /**
     * 检查路径是否匹配模式
     */
    private boolean matchesPattern(String path, String pattern) {
        // 简单的通配符匹配，支持*通配符
        if (pattern.equals("*")) {
            return true;
        }
        
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", ".*");
            return path.matches(regex);
        }
        
        return path.equals(pattern);
    }

    /**
     * 脱敏API密钥
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}