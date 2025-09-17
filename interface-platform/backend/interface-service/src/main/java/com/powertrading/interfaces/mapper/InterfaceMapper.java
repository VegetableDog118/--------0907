package com.powertrading.interfaces.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.interfaces.dto.InterfaceQueryRequest;
import com.powertrading.interfaces.entity.Interface;
import com.powertrading.interfaces.vo.InterfaceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 接口Mapper接口
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Mapper
public interface InterfaceMapper extends BaseMapper<Interface> {

    /**
     * 分页查询接口列表（带关联信息）
     *
     * @param page 分页参数
     * @param request 查询条件
     * @return 接口列表
     */
    IPage<InterfaceVO> selectInterfacePage(Page<InterfaceVO> page, @Param("req") InterfaceQueryRequest request);

    /**
     * 根据ID查询接口详情（带关联信息）
     *
     * @param id 接口ID
     * @return 接口详情
     */
    InterfaceVO selectInterfaceById(@Param("id") String id);

    /**
     * 根据接口路径查询接口
     * @param interfacePath 接口路径
     * @return 接口信息
     */
    Interface selectByPath(@Param("interfacePath") String interfacePath);

    /**
     * 根据接口名称查询接口
     * @param interfaceName 接口名称
     * @return 接口信息
     */
    Interface selectByName(@Param("interfaceName") String interfaceName);

    /**
     * 批量更新接口状态
     *
     * @param ids 接口ID列表
     * @param status 目标状态
     * @param operatorId 操作人ID
     * @return 更新数量
     */
    int batchUpdateStatus(@Param("ids") List<String> ids, 
                         @Param("status") String status, 
                         @Param("operatorId") String operatorId);

    /**
     * 根据分类ID查询接口数量
     *
     * @param categoryId 分类ID
     * @return 接口数量
     */
    int countByCategoryId(@Param("categoryId") String categoryId);

    /**
     * 根据数据源ID查询接口数量
     *
     * @param dataSourceId 数据源ID
     * @return 接口数量
     */
    int countByDataSourceId(@Param("dataSourceId") String dataSourceId);

    /**
     * 查询已发布的接口列表
     *
     * @return 已发布接口列表
     */
    List<InterfaceVO> selectPublishedInterfaces();

    /**
     * 根据状态查询接口列表
     *
     * @param status 接口状态
     * @return 接口列表
     */
    List<Interface> selectByStatus(@Param("status") String status);

    /**
     * 查询接口统计信息
     *
     * @return 统计信息
     */
    List<InterfaceStatistics> selectInterfaceStatistics();

    /**
     * 接口统计信息内部类
     */
    class InterfaceStatistics {
        private String status;
        private Long count;
        private String categoryId;
        private String categoryName;
        
        // getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
        public String getCategoryId() { return categoryId; }
        public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    }
}