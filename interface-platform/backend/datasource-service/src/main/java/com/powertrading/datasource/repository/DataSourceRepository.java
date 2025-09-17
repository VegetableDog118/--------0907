package com.powertrading.datasource.repository;

import com.powertrading.datasource.entity.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 数据源仓库接口
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long>, JpaSpecificationExecutor<DataSource> {

    /**
     * 根据名称查找数据源
     */
    Optional<DataSource> findByName(String name);

    /**
     * 根据类型查找数据源列表
     */
    List<DataSource> findByType(String type);

    /**
     * 根据状态查找数据源列表
     */
    List<DataSource> findByStatus(Integer status);

    /**
     * 根据健康状态查找数据源列表
     */
    List<DataSource> findByHealthStatus(Integer healthStatus);

    /**
     * 查找启用的数据源列表
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.status = 1 ORDER BY ds.name")
    List<DataSource> findEnabledDataSources();

    /**
     * 查找需要健康检查的数据源
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.status = 1 AND " +
           "(ds.lastHealthCheckAt IS NULL OR ds.lastHealthCheckAt < :threshold)")
    List<DataSource> findDataSourcesNeedHealthCheck(@Param("threshold") LocalDateTime threshold);

    /**
     * 根据类型和状态查找数据源
     */
    List<DataSource> findByTypeAndStatus(String type, Integer status);

    /**
     * 检查数据源名称是否存在（排除指定ID）
     */
    @Query("SELECT COUNT(ds) > 0 FROM DataSource ds WHERE ds.name = :name AND ds.id != :id")
    boolean existsByNameAndIdNot(@Param("name") String name, @Param("id") Long id);

    /**
     * 更新数据源的最后连接时间
     */
    @Modifying
    @Query("UPDATE DataSource ds SET ds.lastConnectedAt = :connectedAt WHERE ds.id = :id")
    void updateLastConnectedAt(@Param("id") Long id, @Param("connectedAt") LocalDateTime connectedAt);

    /**
     * 更新数据源的健康检查信息
     */
    @Modifying
    @Query("UPDATE DataSource ds SET ds.lastHealthCheckAt = :checkAt, ds.healthStatus = :healthStatus, " +
           "ds.errorMessage = :errorMessage WHERE ds.id = :id")
    void updateHealthCheckInfo(@Param("id") Long id, 
                              @Param("checkAt") LocalDateTime checkAt,
                              @Param("healthStatus") Integer healthStatus,
                              @Param("errorMessage") String errorMessage);

    /**
     * 批量更新数据源状态
     */
    @Modifying
    @Query("UPDATE DataSource ds SET ds.status = :status WHERE ds.id IN :ids")
    void updateStatusByIds(@Param("ids") List<Long> ids, @Param("status") Integer status);

    /**
     * 统计各类型数据源数量
     */
    @Query("SELECT ds.type, COUNT(ds) FROM DataSource ds GROUP BY ds.type")
    List<Object[]> countByType();

    /**
     * 统计各状态数据源数量
     */
    @Query("SELECT ds.status, COUNT(ds) FROM DataSource ds GROUP BY ds.status")
    List<Object[]> countByStatus();

    /**
     * 查找创建时间在指定范围内的数据源
     */
    List<DataSource> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找指定用户创建的数据源
     */
    List<DataSource> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * 删除指定状态的数据源（软删除）
     */
    @Modifying
    @Query("UPDATE DataSource ds SET ds.status = 0 WHERE ds.status = :oldStatus")
    void softDeleteByStatus(@Param("oldStatus") Integer oldStatus);
}