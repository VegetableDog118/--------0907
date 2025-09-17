package com.powertrading.notification.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket事件监听器
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@Component
public class WebSocketEventListener {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_USERS_KEY = "notification:online:users";
    private static final String USER_SESSION_KEY = "notification:session:";

    /**
     * 用户连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            
            // 从请求头中获取用户ID（实际项目中可能需要从JWT token中解析）
            String userId = headerAccessor.getFirstNativeHeader("userId");
            
            if (userId != null) {
                // 记录用户会话
                String sessionKey = USER_SESSION_KEY + sessionId;
                redisTemplate.opsForValue().set(sessionKey, userId, 24, TimeUnit.HOURS);
                
                // 添加到在线用户集合
                redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
                redisTemplate.expire(ONLINE_USERS_KEY, 24, TimeUnit.HOURS);
                
                log.info("用户连接WebSocket成功，用户ID：{}, 会话ID：{}", userId, sessionId);
            }
        } catch (Exception e) {
            log.error("处理WebSocket连接事件失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 用户断开连接事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            
            // 从Redis中获取用户ID
            String sessionKey = USER_SESSION_KEY + sessionId;
            Object userIdObj = redisTemplate.opsForValue().get(sessionKey);
            
            if (userIdObj != null) {
                String userId = userIdObj.toString();
                
                // 删除会话记录
                redisTemplate.delete(sessionKey);
                
                // 检查用户是否还有其他活跃会话
                boolean hasOtherSessions = hasOtherActiveSessions(userId, sessionId);
                
                if (!hasOtherSessions) {
                    // 从在线用户集合中移除
                    redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
                    log.info("用户断开WebSocket连接，用户ID：{}, 会话ID：{}", userId, sessionId);
                } else {
                    log.info("用户断开WebSocket连接，但仍有其他活跃会话，用户ID：{}, 会话ID：{}", userId, sessionId);
                }
            }
        } catch (Exception e) {
            log.error("处理WebSocket断开连接事件失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 检查用户是否还有其他活跃会话
     */
    private boolean hasOtherActiveSessions(String userId, String currentSessionId) {
        try {
            // 查找所有会话键
            String pattern = USER_SESSION_KEY + "*";
            var keys = redisTemplate.keys(pattern);
            
            if (keys != null) {
                for (String key : keys) {
                    // 跳过当前会话
                    if (key.endsWith(currentSessionId)) {
                        continue;
                    }
                    
                    Object sessionUserId = redisTemplate.opsForValue().get(key);
                    if (userId.equals(sessionUserId)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("检查用户活跃会话失败，用户ID：{}, 错误：{}", userId, e.getMessage(), e);
        }
        return false;
    }
}