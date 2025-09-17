package com.powertrading.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.user.dto.*;
import com.powertrading.user.entity.User;
import com.powertrading.user.mapper.UserMapper;
import com.powertrading.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.secret:powertrading-interface-platform-secret-key}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400}")
    private Long jwtExpiration;

    private static final String REDIS_USER_PREFIX = "user:";
    private static final String REDIS_TOKEN_PREFIX = "token:";
    private static final String REDIS_LOGIN_FAIL_PREFIX = "login_fail:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 30;

    @Override
    public UserRegisterResponse register(UserRegisterRequest request) {
        logger.info("用户注册请求: {}", request.getPhone());

        // 检查用户名是否已存在（使用手机号作为用户名）
        if (userMapper.selectOne(new QueryWrapper<User>().eq("username", request.getPhone())) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查手机号是否已存在
        if (StringUtils.hasText(request.getPhone()) && 
            userMapper.findByPhone(request.getPhone()) != null) {
            throw new RuntimeException("手机号已被注册");
        }

        // 检查邮箱是否已存在
        if (StringUtils.hasText(request.getEmail()) && 
            userMapper.findByEmail(request.getEmail()) != null) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 创建用户
        User user = new User();
        // user.setUserId() is not needed as User entity uses @TableId with ASSIGN_UUID
        user.setUsername(request.getPhone()); // 使用手机号作为用户名
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCompanyName(request.getCompanyName());
        user.setCreditCode(request.getCreditCode());
        user.setContactName(request.getContactName());
        user.setRealName(request.getContactName()); // 使用联系人姓名作为真实姓名
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        user.setPosition(request.getPosition());
        user.setRole("USER");
        user.setStatus("PENDING");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 生成API密钥
        user.setAppId(generateAppId());
        user.setAppSecret(generateAppSecret());

        userMapper.insert(user);

        logger.info("用户注册成功: {}", user.getId());
        return new UserRegisterResponse(user.getId(), user.getStatus(), user.getCreatedAt(), "注册成功，等待审核");
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        logger.info("用户登录请求: {}", request.getAccount());

        // 检查登录失败次数
        String failKey = REDIS_LOGIN_FAIL_PREFIX + request.getAccount();
        Integer failCount = (Integer) redisTemplate.opsForValue().get(failKey);
        if (failCount != null && failCount >= MAX_LOGIN_ATTEMPTS) {
            throw new RuntimeException("登录失败次数过多，账户已被锁定30分钟");
        }

        // 查找用户
        User user = findUserByLoginAccount(request.getAccount());
        if (user == null) {
            incrementLoginFailCount(request.getAccount());
            throw new RuntimeException("用户不存在");
        }

        // 检查用户状态
        if ("LOCKED".equals(user.getStatus())) {
            throw new RuntimeException("账户已被锁定，请联系管理员");
        }
        if ("PENDING".equals(user.getStatus())) {
            throw new RuntimeException("账户待审核，请等待管理员审核");
        }
        if ("REJECTED".equals(user.getStatus())) {
            throw new RuntimeException("账户审核未通过，请联系管理员");
        }

        // 验证密码
        boolean passwordMatches;
        
        // 如果数据库密码是明文，直接比较；否则使用BCrypt
        if (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$") || user.getPassword().startsWith("$2y$")) {
            passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        } else {
            // 明文密码比较（临时兼容）
            passwordMatches = request.getPassword().equals(user.getPassword());
        }
        
        if (!passwordMatches) {
            incrementLoginFailCount(request.getAccount());
            throw new RuntimeException("密码错误");
        }

        // 清除登录失败记录
        redisTemplate.delete(failKey);

        // 更新最后登录信息
        userMapper.updateLastLoginInfo(user.getId(), LocalDateTime.now(), getClientIp());

        // 生成JWT Token
        TokenInfo tokenInfo = generateToken(user);

        // 缓存用户信息
        cacheUserInfo(user);

        // 构建响应
        UserLoginResponse response = new UserLoginResponse();
        response.setToken(tokenInfo.getToken());
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtExpiration);
        
        // 设置用户信息
        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setUserId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setCompanyName(user.getCompanyName());
        userInfo.setRole(user.getRole());
        userInfo.setStatus(user.getStatus());
        response.setUserInfo(userInfo);
        
        response.setPermissions(getUserPermissions(user.getRole()));

        logger.info("用户登录成功: {}", user.getId());
        return response;
    }

    @Override
    public UserInfoResponse getUserInfo(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setCompanyName(user.getCompanyName());
        // response.setCreditCode(user.getCreditCode()); // UserInfoResponse may not have this field
        response.setContactName(user.getContactName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setDepartment(user.getDepartment());
        response.setPosition(user.getPosition());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginTime(user.getLastLoginTime());

        return response;
    }

    @Override
    public UserInfoResponse getUserByAppId(String appId) {
        User user = userMapper.findByAppId(appId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return getUserInfo(user.getId());
    }

    @Override
    public void updateUserInfo(String userId, UserUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查手机号是否被其他用户使用
        if (StringUtils.hasText(request.getPhone())) {
            User existingUser = userMapper.findByPhone(request.getPhone());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new RuntimeException("手机号已被其他用户使用");
            }
        }

        // 检查邮箱是否被其他用户使用
        if (StringUtils.hasText(request.getEmail())) {
            User existingUser = userMapper.findByEmail(request.getEmail());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new RuntimeException("邮箱已被其他用户使用");
            }
        }

        // 更新用户信息
        user.setContactName(request.getContactName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        user.setPosition(request.getPosition());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);

        // 更新缓存
        cacheUserInfo(user);

        logger.info("用户信息更新成功: {}", userId);
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证原密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 验证新密码确认
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("新密码与确认密码不一致");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        logger.info("用户密码修改成功: {}", userId);
    }

    @Override
    public ApiKeyResponse resetApiKey(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 生成新的API密钥
        String newAppId = generateAppId();
        String newAppSecret = generateAppSecret();

        user.setAppId(newAppId);
        user.setAppSecret(newAppSecret);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 更新缓存
        cacheUserInfo(user);

        logger.info("API密钥重置成功: {}", userId);
        return new ApiKeyResponse(newAppId, newAppSecret, "ACTIVE");
    }

    @Override
    public ApiKeyResponse getApiKey(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        ApiKeyResponse response = new ApiKeyResponse();
        response.setAppId(user.getAppId());
        response.setAppSecret(maskSecret(user.getAppSecret()));
        response.setStatus("ACTIVE".equals(user.getStatus()) ? "ACTIVE" : "INACTIVE");
        response.setCreateTime(user.getCreatedAt());
        response.setLastUsedTime(user.getLastLoginTime());
        response.setPermissions(getUserPermissions(user.getRole()));

        return response;
    }

    @Override
    public List<String> getUserPermissions(String role) {
        List<String> permissions = new ArrayList<>();
        
        switch (role) {
            case "admin":
                permissions.addAll(Arrays.asList(
                    "user:read", "user:write", "user:delete",
                    "interface:read", "interface:write", "interface:delete",
                    "datasource:read", "datasource:write", "datasource:delete",
                    "approval:read", "approval:write",
                    "system:read", "system:write",
                    "user:read:self", "user:write:self"
                ));
                break;
            case "settlement":
                permissions.addAll(Arrays.asList(
                    "interface:read", "interface:write", "interface:publish",
                    "approval:read", "approval:write", "approval:process",
                    "user:read", "statistics:read",
                    "user:read:self", "user:write:self"
                ));
                break;
            case "tech":
                permissions.addAll(Arrays.asList(
                    "interface:read", "interface:write", "interface:create",
                    "datasource:read", "datasource:write",
                    "interface:test", "interface:debug",
                    "user:read:self", "user:write:self"
                ));
                break;
            case "consumer":
                permissions.addAll(Arrays.asList(
                    "interface:read", "interface:call", "interface:subscribe",
                    "application:create", "application:read",
                    "user:read:self", "user:write:self"
                ));
                break;
            case "USER":
            case "user":
                permissions.addAll(Arrays.asList(
                    "interface:read", "interface:call",
                    "user:read:self", "user:write:self"
                ));
                break;
            default:
                permissions.add("user:read:self");
        }
        
        return permissions;
    }

    @Override
    public void approveUser(String userId, String approvedBy) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setStatus("ACTIVE");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        logger.info("用户审核通过: {} by {}", userId, approvedBy);
    }

    @Override
    public void rejectUser(String userId, String rejectedBy, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setStatus("REJECTED");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        logger.info("用户审核拒绝: {} by {} reason: {}", userId, rejectedBy, reason);
    }

    @Override
    public void lockUser(String userId, String lockedBy, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        userMapper.lockUser(userId, LocalDateTime.now().plusHours(24)); // 锁定24小时
        logger.info("用户锁定: {} by {} reason: {}", userId, lockedBy, reason);
    }

    @Override
    public void unlockUser(String userId, String unlockedBy) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        userMapper.unlockUser(userId);
        logger.info("用户解锁: {} by {}", userId, unlockedBy);
    }

    @Override
    public PageResponse<UserInfoResponse> getUserList(int page, int size, String status, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(status)) {
            queryWrapper.eq("status", status);
        }
        
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like("username", keyword)
                .or().like("company_name", keyword)
                .or().like("contact_name", keyword)
            );
        }
        
        queryWrapper.orderByDesc("created_at");
        
        IPage<User> userPage = userMapper.selectPage(pageParam, queryWrapper);
        
        List<UserInfoResponse> records = userPage.getRecords().stream()
            .map(this::convertToUserInfoResponse)
            .collect(Collectors.toList());
        
        return new PageResponse<>(records, userPage.getTotal(), userPage.getCurrent(), userPage.getSize());
    }

    @Override
    public TokenInfo validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

            TokenInfo tokenInfo = new TokenInfo();
            tokenInfo.setUserId(claims.getSubject());
            tokenInfo.setUsername(claims.get("username", String.class));
            tokenInfo.setCompanyName(claims.get("companyName", String.class));
            tokenInfo.setTokenType("Bearer");
            tokenInfo.setIssuedAt(LocalDateTime.ofEpochSecond(claims.getIssuedAt().getTime() / 1000, 0, java.time.ZoneOffset.UTC));
            tokenInfo.setExpiresAt(LocalDateTime.ofEpochSecond(claims.getExpiration().getTime() / 1000, 0, java.time.ZoneOffset.UTC));
            
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            tokenInfo.setRoles(roles);
            
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);
            tokenInfo.setPermissions(permissions);

            return tokenInfo;
        } catch (Exception e) {
            logger.error("Token验证失败: {}", e.getMessage());
            throw new RuntimeException("Token无效");
        }
    }

    @Override
    public void logout(String token) {
        // 将token加入黑名单
        String tokenKey = REDIS_TOKEN_PREFIX + "blacklist:" + token;
        redisTemplate.opsForValue().set(tokenKey, "blacklisted", jwtExpiration, TimeUnit.SECONDS);
        
        logger.info("用户登出，Token已加入黑名单");
    }

    @Override
    public void handleLoginSuccess(User user, String clientIp) {
        // 更新最后登录信息
        userMapper.updateLastLoginInfo(user.getId(), LocalDateTime.now(), clientIp);
        
        // 清除登录失败记录
        String failKey = REDIS_LOGIN_FAIL_PREFIX + user.getUsername();
        redisTemplate.delete(failKey);
        
        logger.info("用户登录成功处理完成: {}", user.getId());
    }

    @Override
    public void handleLoginFailure(User user) {
        // 增加登录失败次数
        int currentAttempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
        currentAttempts++;
        
        userMapper.updateFailedLoginAttempts(user.getId(), currentAttempts);
        
        // 如果失败次数达到上限，锁定账户
        if (currentAttempts >= MAX_LOGIN_ATTEMPTS) {
            userMapper.lockUser(user.getId(), LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES));
            logger.warn("用户账户因登录失败次数过多被锁定: {}", user.getId());
        }
        
        logger.warn("用户登录失败: {} 失败次数: {}", user.getId(), currentAttempts);
    }

    @Override
    public boolean isAccountLocked(User user) {
        // 检查账户状态
        if ("LOCKED".equals(user.getStatus())) {
            return true;
        }
        
        // 检查是否因登录失败被临时锁定
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            return true;
        }
        
        return false;
    }

    @Override
    public User validateCredentials(String account, String password) {
        // 查找用户
        User user = findUserByLoginAccount(account);
        if (user == null) {
            return null;
        }
        
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }
        
        return user;
    }

    @Override
    public boolean hasPermission(String userId, String permission) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        
        List<String> userPermissions = getUserPermissions(user.getRole());
        return userPermissions.contains(permission);
    }

    @Override
    public boolean userExists(String userId) {
        User user = userMapper.selectById(userId);
        return user != null;
    }

    @Override
    public TokenInfo refreshToken(String token) {
        try {
            // 验证旧token
            TokenInfo tokenInfo = validateToken(token);
            
            // 根据用户ID获取用户信息
            User user = userMapper.selectById(tokenInfo.getUserId());
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            
            // 生成新token
            TokenInfo newTokenInfo = generateToken(user);
            
            // 将旧token加入黑名单
            String tokenKey = REDIS_TOKEN_PREFIX + "blacklist:" + token;
            redisTemplate.opsForValue().set(tokenKey, "blacklisted", jwtExpiration, TimeUnit.SECONDS);
            
            logger.info("Token刷新成功: {}", user.getId());
            return newTokenInfo;
        } catch (Exception e) {
            logger.error("Token刷新失败: {}", e.getMessage());
            throw new RuntimeException("Token刷新失败");
        }
    }

    // 私有辅助方法
    private User findUserByLoginAccount(String loginAccount) {
        // 尝试用户名查找
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", loginAccount));
        if (user != null) return user;
        
        // 尝试手机号查找
        user = userMapper.findByPhone(loginAccount);
        if (user != null) return user;
        
        // 尝试邮箱查找
        return userMapper.findByEmail(loginAccount);
    }

    private void incrementLoginFailCount(String loginAccount) {
        String failKey = REDIS_LOGIN_FAIL_PREFIX + loginAccount;
        Integer failCount = (Integer) redisTemplate.opsForValue().get(failKey);
        failCount = failCount == null ? 1 : failCount + 1;
        
        redisTemplate.opsForValue().set(failKey, failCount, LOCK_TIME_MINUTES, TimeUnit.MINUTES);
        
        logger.warn("登录失败次数增加: {} - {}", loginAccount, failCount);
    }

    public TokenInfo generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpiration * 1000);

        String token = Jwts.builder()
            .setSubject(user.getId())
            .claim("username", user.getUsername())
            .claim("companyName", user.getCompanyName())
            .claim("roles", Arrays.asList(user.getRole()))
            .claim("permissions", getUserPermissions(user.getRole()))
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
            
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setUserId(user.getId());
        tokenInfo.setUsername(user.getUsername());
        tokenInfo.setCompanyName(user.getCompanyName());
        tokenInfo.setTokenType("Bearer");
        tokenInfo.setToken(token);
        tokenInfo.setIssuedAt(LocalDateTime.ofInstant(now.toInstant(), java.time.ZoneOffset.UTC));
        tokenInfo.setExpiresAt(LocalDateTime.ofInstant(expiration.toInstant(), java.time.ZoneOffset.UTC));
        tokenInfo.setRoles(Arrays.asList(user.getRole()));
        tokenInfo.setPermissions(getUserPermissions(user.getRole()));
        
        return tokenInfo;
    }

    private void cacheUserInfo(User user) {
        String userKey = REDIS_USER_PREFIX + user.getId();
        redisTemplate.opsForValue().set(userKey, user, 24, TimeUnit.HOURS);
    }

    private String generateAppId() {
        return "app_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateAppSecret() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.length() < 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }

    private String getClientIp() {
        // 这里应该从请求上下文获取客户端IP，简化处理
        return "127.0.0.1";
    }

    private UserInfoResponse convertToUserInfoResponse(User user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setCompanyName(user.getCompanyName());
        // response.setCreditCode(user.getCreditCode()); // UserInfoResponse doesn't have this field
        response.setContactName(user.getContactName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setDepartment(user.getDepartment());
        response.setPosition(user.getPosition());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginTime(user.getLastLoginTime());
        return response;
    }
}