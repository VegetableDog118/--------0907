package com.powertrading.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 通知Mapper接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 分页查询用户通知
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @param type   通知类型
     * @param status 阅读状态
     * @return 通知列表
     */
    @Select("<script>" +
            "SELECT * FROM notifications WHERE user_id = #{userId}" +
            "<if test=\"type != null and type != ''\">" +
            " AND type = #{type}" +
            "</if>" +
            "<if test=\"status != null and status != ''\">" +
            " AND status = #{status}" +
            "</if>" +
            " ORDER BY create_time DESC" +
            "</script>")
    IPage<Notification> selectUserNotifications(Page<Notification> page,
                                                @Param("userId") String userId,
                                                @Param("type") String type,
                                                @Param("status") String status);

    /**
     * 统计用户未读通知数量
     *
     * @param userId 用户ID
     * @return 未读通知数量
     */
    @Select("SELECT COUNT(*) FROM notifications WHERE user_id = #{userId} AND status = 'unread'")
    Long countUnreadByUserId(@Param("userId") String userId);

    /**
     * 批量标记为已读
     *
     * @param userId          用户ID
     * @param notificationIds 通知ID列表
     * @param readTime        阅读时间
     * @return 更新数量
     */
    @Update("<script>" +
            "UPDATE notifications SET status = 'read', read_time = #{readTime}" +
            " WHERE user_id = #{userId} AND id IN" +
            "<foreach collection='notificationIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchMarkAsRead(@Param("userId") String userId,
                        @Param("notificationIds") List<String> notificationIds,
                        @Param("readTime") LocalDateTime readTime);

    /**
     * 标记所有未读通知为已读
     *
     * @param userId   用户ID
     * @param readTime 阅读时间
     * @return 更新数量
     */
    @Update("UPDATE notifications SET status = 'read', read_time = #{readTime} WHERE user_id = #{userId} AND status = 'unread'")
    int markAllAsRead(@Param("userId") String userId, @Param("readTime") LocalDateTime readTime);

    /**
     * 删除过期通知
     *
     * @param expireTime 过期时间
     * @return 删除数量
     */
    @Update("DELETE FROM notifications WHERE create_time < #{expireTime}")
    int deleteExpiredNotifications(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 统计通知数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计数据
     */
    @Select("SELECT type, status, COUNT(*) as count FROM notifications " +
            "WHERE create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY type, status")
    List<Map<String, Object>> getNotificationStatistics(@Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 获取用户通知统计
     *
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计数据
     */
    @Select("SELECT type, status, COUNT(*) as count FROM notifications " +
            "WHERE user_id = #{userId} AND create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY type, status")
    List<Map<String, Object>> getUserNotificationStatistics(@Param("userId") String userId,
                                                             @Param("startTime") LocalDateTime startTime,
                                                             @Param("endTime") LocalDateTime endTime);
}