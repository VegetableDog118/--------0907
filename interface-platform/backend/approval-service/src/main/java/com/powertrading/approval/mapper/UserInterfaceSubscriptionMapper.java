package com.powertrading.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.approval.entity.UserInterfaceSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户接口订阅Mapper接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Mapper
public interface UserInterfaceSubscriptionMapper extends BaseMapper<UserInterfaceSubscription> {

    /**
     * 分页查询用户订阅列表
     *
     * @param page 分页参数
     * @param userId 用户ID（可选）
     * @param interfaceId 接口ID（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT * FROM user_interface_subscriptions " +
            "WHERE 1=1 " +
            "<if test=\"userId != null and userId != ''\">" +
            "AND user_id = #{userId} " +
            "</if>" +
            "<if test=\"interfaceId != null and interfaceId != ''\">" +
            "AND interface_id = #{interfaceId} " +
            "</if>" +
            "<if test=\"status != null and status != ''\">" +
            "AND status = #{status} " +
            "</if>" +
            "ORDER BY subscribe_time DESC" +
            "</script>")
    IPage<UserInterfaceSubscription> selectSubscriptionPage(
            Page<UserInterfaceSubscription> page,
            @Param("userId") String userId,
            @Param("interfaceId") String interfaceId,
            @Param("status") String status
    );

    /**
     * 根据用户ID和接口ID查询订阅记录
     *
     * @param userId 用户ID
     * @param interfaceId 接口ID
     * @return 订阅记录
     */
    @Select("SELECT * FROM user_interface_subscriptions " +
            "WHERE user_id = #{userId} AND interface_id = #{interfaceId}")
    UserInterfaceSubscription selectByUserAndInterface(
            @Param("userId") String userId,
            @Param("interfaceId") String interfaceId
    );

    /**
     * 根据申请ID查询订阅记录
     *
     * @param applicationId 申请ID
     * @return 订阅记录列表
     */
    @Select("SELECT * FROM user_interface_subscriptions " +
            "WHERE application_id = #{applicationId}")
    List<UserInterfaceSubscription> selectByApplicationId(@Param("applicationId") String applicationId);

    /**
     * 批量插入订阅记录
     *
     * @param subscriptions 订阅记录列表
     * @return 插入数量
     */
    int batchInsert(@Param("subscriptions") List<UserInterfaceSubscription> subscriptions);

    /**
     * 更新调用次数
     *
     * @param userId 用户ID
     * @param interfaceId 接口ID
     * @param callCount 调用次数增量
     * @param lastCallTime 最后调用时间
     * @return 更新数量
     */
    @Update("UPDATE user_interface_subscriptions SET " +
            "call_count = call_count + #{callCount}, " +
            "last_call_time = #{lastCallTime}, " +
            "update_time = NOW() " +
            "WHERE user_id = #{userId} AND interface_id = #{interfaceId}")
    int updateCallCount(
            @Param("userId") String userId,
            @Param("interfaceId") String interfaceId,
            @Param("callCount") Integer callCount,
            @Param("lastCallTime") LocalDateTime lastCallTime
    );

    /**
     * 批量更新订阅状态
     *
     * @param userIds 用户ID列表
     * @param interfaceIds 接口ID列表
     * @param status 新状态
     * @return 更新数量
     */
    @Update("<script>" +
            "UPDATE user_interface_subscriptions SET " +
            "status = #{status}, " +
            "update_time = NOW() " +
            "WHERE user_id IN " +
            "<foreach collection='userIds' item='userId' open='(' separator=',' close=')'>" +
            "#{userId}" +
            "</foreach>" +
            "AND interface_id IN " +
            "<foreach collection='interfaceIds' item='interfaceId' open='(' separator=',' close=')'>" +
            "#{interfaceId}" +
            "</foreach>" +
            "</script>")
    int batchUpdateStatus(
            @Param("userIds") List<String> userIds,
            @Param("interfaceIds") List<String> interfaceIds,
            @Param("status") String status
    );

    /**
     * 查询即将过期的订阅
     *
     * @param expireTime 过期时间阈值
     * @return 即将过期的订阅列表
     */
    @Select("SELECT * FROM user_interface_subscriptions " +
            "WHERE status = 'active' " +
            "AND expire_time IS NOT NULL " +
            "AND expire_time <= #{expireTime}")
    List<UserInterfaceSubscription> selectExpiringSoon(@Param("expireTime") LocalDateTime expireTime);
}