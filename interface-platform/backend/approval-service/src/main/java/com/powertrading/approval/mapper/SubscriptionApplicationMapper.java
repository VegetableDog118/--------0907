package com.powertrading.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.approval.entity.SubscriptionApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订阅申请Mapper接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Mapper
public interface SubscriptionApplicationMapper extends BaseMapper<SubscriptionApplication> {

    /**
     * 分页查询申请列表
     *
     * @param page 分页参数
     * @param userId 用户ID（可选）
     * @param status 状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 分页结果
     */
    IPage<SubscriptionApplication> selectApplicationPage(
            Page<SubscriptionApplication> page,
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据用户ID和接口ID查询是否已存在申请
     *
     * @param userId 用户ID
     * @param interfaceIds 接口ID列表
     * @return 已存在的申请列表
     */
    List<SubscriptionApplication> selectExistingApplications(
            @Param("userId") String userId,
            @Param("interfaceIds") String interfaceIds
    );

    /**
     * 统计申请数量
     *
     * @param status 状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 统计结果
     */
    Map<String, Object> selectApplicationStatistics(
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 批量更新申请状态
     *
     * @param ids 申请ID列表
     * @param status 新状态
     * @param processBy 处理人
     * @param processComment 处理意见
     * @param processTime 处理时间
     * @return 更新数量
     */
    int batchUpdateStatus(
            @Param("ids") List<String> ids,
            @Param("status") String status,
            @Param("processBy") String processBy,
            @Param("processComment") String processComment,
            @Param("processTime") LocalDateTime processTime
    );
}