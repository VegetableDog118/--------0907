package com.powertrading.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.powertrading.notification.entity.Notification;

import java.util.List;
import java.util.Map;

/**
 * 站内消息服务接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
public interface MessageService {

    /**
     * 发送站内消息
     *
     * @param userId  接收用户ID
     * @param title   消息标题
     * @param content 消息内容
     * @param type    消息类型
     * @return 消息ID
     */
    String sendMessage(String userId, String title, String content, String type);

    /**
     * 批量发送站内消息
     *
     * @param userIds 接收用户ID列表
     * @param title   消息标题
     * @param content 消息内容
     * @param type    消息类型
     * @return 发送结果
     */
    Map<String, String> batchSendMessage(List<String> userIds, String title, String content, String type);

    /**
     * 分页查询用户消息
     *
     * @param userId   用户ID
     * @param type     消息类型
     * @param status   阅读状态
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 消息分页数据
     */
    IPage<Notification> getUserMessages(String userId, String type, String status, int pageNum, int pageSize);

    /**
     * 获取用户未读消息数量
     *
     * @param userId 用户ID
     * @return 未读消息数量
     */
    Long getUnreadCount(String userId);

    /**
     * 标记消息为已读
     *
     * @param userId         用户ID
     * @param notificationId 消息ID
     * @return 是否成功
     */
    boolean markAsRead(String userId, String notificationId);

    /**
     * 批量标记消息为已读
     *
     * @param userId          用户ID
     * @param notificationIds 消息ID列表
     * @return 标记成功的数量
     */
    int batchMarkAsRead(String userId, List<String> notificationIds);

    /**
     * 标记所有未读消息为已读
     *
     * @param userId 用户ID
     * @return 标记成功的数量
     */
    int markAllAsRead(String userId);

    /**
     * 删除消息
     *
     * @param userId         用户ID
     * @param notificationId 消息ID
     * @return 是否成功
     */
    boolean deleteMessage(String userId, String notificationId);

    /**
     * 批量删除消息
     *
     * @param userId          用户ID
     * @param notificationIds 消息ID列表
     * @return 删除成功的数量
     */
    int batchDeleteMessage(String userId, List<String> notificationIds);

    /**
     * 获取消息详情
     *
     * @param userId         用户ID
     * @param notificationId 消息ID
     * @return 消息详情
     */
    Notification getMessageDetail(String userId, String notificationId);

    /**
     * 清理过期消息
     *
     * @param days 保留天数
     * @return 清理数量
     */
    int cleanExpiredMessages(int days);

    /**
     * 推送实时消息（WebSocket）
     *
     * @param userId      用户ID
     * @param notification 通知消息
     * @return 是否推送成功
     */
    boolean pushRealTimeMessage(String userId, Notification notification);

    /**
     * 广播消息给所有在线用户
     *
     * @param title   消息标题
     * @param content 消息内容
     * @param type    消息类型
     * @return 广播结果
     */
    Map<String, Object> broadcastMessage(String title, String content, String type);
}