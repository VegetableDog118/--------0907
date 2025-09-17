package com.powertrading.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.notification.entity.Notification;
import com.powertrading.notification.mapper.NotificationMapper;
import com.powertrading.notification.service.MessageService;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 站内消息服务实现类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Value("${notification.message.max-unread:100}")
    private int maxUnreadMessages;

    @Value("${notification.message.auto-delete-days:30}")
    private int autoDeleteDays;

    private static final String UNREAD_COUNT_KEY = "notification:unread:";
    private static final String ONLINE_USERS_KEY = "notification:online:users";

    @Override
    @Transactional
    public String sendMessage(String userId, String title, String content, String type) {
        try {
            // 检查用户未读消息数量
            Long unreadCount = getUnreadCount(userId);
            if (unreadCount >= maxUnreadMessages) {
                log.warn("用户 {} 未读消息数量已达上限：{}", userId, maxUnreadMessages);
                return null;
            }

            // 创建通知消息
            Notification notification = new Notification()
                    .setId(IdUtil.simpleUUID())
                    .setUserId(userId)
                    .setTitle(title)
                    .setContent(content)
                    .setType(type)
                    .setStatus(Notification.Status.UNREAD.getCode())
                    .setSendTime(LocalDateTime.now());

            // 保存到数据库
            int result = notificationMapper.insert(notification);
            if (result > 0) {
                // 更新Redis中的未读数量
                String unreadKey = UNREAD_COUNT_KEY + userId;
                redisTemplate.opsForValue().increment(unreadKey);
                redisTemplate.expire(unreadKey, 7, TimeUnit.DAYS);

                // 推送实时消息
                pushRealTimeMessage(userId, notification);

                log.info("站内消息发送成功，用户：{}, 标题：{}", userId, title);
                return notification.getId();
            }
        } catch (Exception e) {
            log.error("站内消息发送失败，用户：{}, 标题：{}, 错误：{}", userId, title, e.getMessage(), e);
        }
        return null;
    }

    @Override
    @Transactional
    public Map<String, String> batchSendMessage(List<String> userIds, String title, String content, String type) {
        Map<String, String> results = new HashMap<>();
        
        if (userIds == null || userIds.isEmpty()) {
            return results;
        }

        for (String userId : userIds) {
            String messageId = sendMessage(userId, title, content, type);
            results.put(userId, messageId);
        }

        log.info("批量站内消息发送完成，总数：{}, 成功：{}", 
                userIds.size(), 
                results.values().stream().mapToInt(id -> id != null ? 1 : 0).sum());
        
        return results;
    }

    @Override
    public IPage<Notification> getUserMessages(String userId, String type, String status, int pageNum, int pageSize) {
        Page<Notification> page = new Page<>(pageNum, pageSize);
        return notificationMapper.selectUserNotifications(page, userId, type, status);
    }

    @Override
    public Long getUnreadCount(String userId) {
        String unreadKey = UNREAD_COUNT_KEY + userId;
        Object count = redisTemplate.opsForValue().get(unreadKey);
        
        if (count != null) {
            return Long.valueOf(count.toString());
        }
        
        // Redis中没有缓存，从数据库查询并缓存
        Long dbCount = notificationMapper.countUnreadByUserId(userId);
        redisTemplate.opsForValue().set(unreadKey, dbCount, 7, TimeUnit.DAYS);
        
        return dbCount;
    }

    @Override
    @Transactional
    public boolean markAsRead(String userId, String notificationId) {
        try {
            // 查询消息是否存在且属于该用户
            LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getId, notificationId)
                    .eq(Notification::getUserId, userId)
                    .eq(Notification::getStatus, Notification.Status.UNREAD.getCode());
            
            Notification notification = notificationMapper.selectOne(wrapper);
            if (notification == null) {
                return false;
            }

            // 更新为已读状态
            notification.setStatus(Notification.Status.READ.getCode())
                    .setReadTime(LocalDateTime.now());
            
            int result = notificationMapper.updateById(notification);
            if (result > 0) {
                // 更新Redis中的未读数量
                String unreadKey = UNREAD_COUNT_KEY + userId;
                redisTemplate.opsForValue().decrement(unreadKey);
                
                log.info("消息标记为已读成功，用户：{}, 消息ID：{}", userId, notificationId);
                return true;
            }
        } catch (Exception e) {
            log.error("消息标记为已读失败，用户：{}, 消息ID：{}, 错误：{}", userId, notificationId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    @Transactional
    public int batchMarkAsRead(String userId, List<String> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return 0;
        }

        try {
            int result = notificationMapper.batchMarkAsRead(userId, notificationIds, LocalDateTime.now());
            if (result > 0) {
                // 更新Redis中的未读数量
                String unreadKey = UNREAD_COUNT_KEY + userId;
                redisTemplate.opsForValue().increment(unreadKey, -result);
                
                log.info("批量标记消息为已读成功，用户：{}, 数量：{}", userId, result);
            }
            return result;
        } catch (Exception e) {
            log.error("批量标记消息为已读失败，用户：{}, 错误：{}", userId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional
    public int markAllAsRead(String userId) {
        try {
            int result = notificationMapper.markAllAsRead(userId, LocalDateTime.now());
            if (result > 0) {
                // 清空Redis中的未读数量
                String unreadKey = UNREAD_COUNT_KEY + userId;
                redisTemplate.opsForValue().set(unreadKey, 0, 7, TimeUnit.DAYS);
                
                log.info("标记所有消息为已读成功，用户：{}, 数量：{}", userId, result);
            }
            return result;
        } catch (Exception e) {
            log.error("标记所有消息为已读失败，用户：{}, 错误：{}", userId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional
    public boolean deleteMessage(String userId, String notificationId) {
        try {
            LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getId, notificationId)
                    .eq(Notification::getUserId, userId);
            
            Notification notification = notificationMapper.selectOne(wrapper);
            if (notification == null) {
                return false;
            }

            int result = notificationMapper.deleteById(notificationId);
            if (result > 0) {
                // 如果删除的是未读消息，更新Redis中的未读数量
                if (Notification.Status.UNREAD.getCode().equals(notification.getStatus())) {
                    String unreadKey = UNREAD_COUNT_KEY + userId;
                    redisTemplate.opsForValue().decrement(unreadKey);
                }
                
                log.info("消息删除成功，用户：{}, 消息ID：{}", userId, notificationId);
                return true;
            }
        } catch (Exception e) {
            log.error("消息删除失败，用户：{}, 消息ID：{}, 错误：{}", userId, notificationId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    @Transactional
    public int batchDeleteMessage(String userId, List<String> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return 0;
        }

        try {
            // 查询要删除的未读消息数量
            LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                    .in(Notification::getId, notificationIds)
                    .eq(Notification::getUserId, userId)
                    .eq(Notification::getStatus, Notification.Status.UNREAD.getCode());
            
            Long unreadCount = notificationMapper.selectCount(wrapper);
            
            // 删除消息
            LambdaQueryWrapper<Notification> deleteWrapper = new LambdaQueryWrapper<Notification>()
                    .in(Notification::getId, notificationIds)
                    .eq(Notification::getUserId, userId);
            
            int result = notificationMapper.delete(deleteWrapper);
            if (result > 0 && unreadCount > 0) {
                // 更新Redis中的未读数量
                String unreadKey = UNREAD_COUNT_KEY + userId;
                redisTemplate.opsForValue().increment(unreadKey, -unreadCount);
            }
            
            log.info("批量删除消息成功，用户：{}, 数量：{}", userId, result);
            return result;
        } catch (Exception e) {
            log.error("批量删除消息失败，用户：{}, 错误：{}", userId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public Notification getMessageDetail(String userId, String notificationId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId);
        
        return notificationMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public int cleanExpiredMessages(int days) {
        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(days);
            int result = notificationMapper.deleteExpiredNotifications(expireTime);
            
            log.info("清理过期消息完成，清理数量：{}, 保留天数：{}", result, days);
            return result;
        } catch (Exception e) {
            log.error("清理过期消息失败，错误：{}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean pushRealTimeMessage(String userId, Notification notification) {
        try {
            if (messagingTemplate != null) {
                // 推送到用户专属频道
                messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
                
                log.debug("实时消息推送成功，用户：{}, 消息ID：{}", userId, notification.getId());
                return true;
            }
        } catch (Exception e) {
            log.error("实时消息推送失败，用户：{}, 消息ID：{}, 错误：{}", 
                    userId, notification.getId(), e.getMessage(), e);
        }
        return false;
    }

    @Override
    @Transactional
    public Map<String, Object> broadcastMessage(String title, String content, String type) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取所有在线用户（从Redis获取）
            @SuppressWarnings("unchecked")
            List<String> onlineUsers = (List<String>) redisTemplate.opsForValue().get(ONLINE_USERS_KEY);
            
            if (onlineUsers == null || onlineUsers.isEmpty()) {
                result.put("success", false);
                result.put("message", "没有在线用户");
                result.put("count", 0);
                return result;
            }

            // 批量发送消息
            Map<String, String> sendResults = batchSendMessage(onlineUsers, title, content, type);
            
            // 广播实时消息
            if (messagingTemplate != null) {
                Map<String, Object> broadcastData = new HashMap<>();
                broadcastData.put("title", title);
                broadcastData.put("content", content);
                broadcastData.put("type", type);
                broadcastData.put("time", LocalDateTime.now());
                
                messagingTemplate.convertAndSend("/topic/broadcast", broadcastData);
            }
            
            long successCount = sendResults.values().stream().mapToLong(id -> id != null ? 1 : 0).sum();
            
            result.put("success", true);
            result.put("message", "广播消息发送完成");
            result.put("totalUsers", onlineUsers.size());
            result.put("successCount", successCount);
            result.put("failCount", onlineUsers.size() - successCount);
            
            log.info("广播消息发送完成，标题：{}, 在线用户：{}, 成功：{}", title, onlineUsers.size(), successCount);
            
        } catch (Exception e) {
            log.error("广播消息发送失败，标题：{}, 错误：{}", title, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "广播消息发送失败：" + e.getMessage());
        }
        
        return result;
    }
}