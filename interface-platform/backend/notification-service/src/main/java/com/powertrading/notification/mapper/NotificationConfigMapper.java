package com.powertrading.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powertrading.notification.entity.NotificationConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 通知配置Mapper接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Mapper
public interface NotificationConfigMapper extends BaseMapper<NotificationConfig> {

    /**
     * 根据用户ID查询通知配置
     *
     * @param userId 用户ID
     * @return 通知配置列表
     */
    @Select("SELECT * FROM notification_configs WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<NotificationConfig> selectByUserId(@Param("userId") String userId);

    /**
     * 根据用户ID和通知类型查询配置
     *
     * @param userId           用户ID
     * @param notificationType 通知类型
     * @return 通知配置
     */
    @Select("SELECT * FROM notification_configs WHERE user_id = #{userId} AND notification_type = #{notificationType}")
    NotificationConfig selectByUserIdAndType(@Param("userId") String userId,
                                             @Param("notificationType") String notificationType);

    /**
     * 检查用户是否启用了指定类型的邮件通知
     *
     * @param userId           用户ID
     * @param notificationType 通知类型
     * @return 是否启用
     */
    @Select("SELECT COALESCE(email_enabled, 1) FROM notification_configs " +
            "WHERE user_id = #{userId} AND notification_type = #{notificationType}")
    Integer isEmailEnabled(@Param("userId") String userId,
                            @Param("notificationType") String notificationType);

    /**
     * 检查用户是否启用了指定类型的站内消息
     *
     * @param userId           用户ID
     * @param notificationType 通知类型
     * @return 是否启用
     */
    @Select("SELECT COALESCE(message_enabled, 1) FROM notification_configs " +
            "WHERE user_id = #{userId} AND notification_type = #{notificationType}")
    Integer isMessageEnabled(@Param("userId") String userId,
                              @Param("notificationType") String notificationType);

    /**
     * 检查用户是否启用了指定类型的短信通知
     *
     * @param userId           用户ID
     * @param notificationType 通知类型
     * @return 是否启用
     */
    @Select("SELECT COALESCE(sms_enabled, 0) FROM notification_configs " +
            "WHERE user_id = #{userId} AND notification_type = #{notificationType}")
    Integer isSmsEnabled(@Param("userId") String userId,
                          @Param("notificationType") String notificationType);
}