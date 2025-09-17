package com.powertrading.interfaces.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powertrading.interfaces.entity.InterfaceParameter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 接口参数Mapper接口
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Mapper
public interface InterfaceParameterMapper extends BaseMapper<InterfaceParameter> {

    /**
     * 根据接口ID查询参数列表
     *
     * @param interfaceId 接口ID
     * @return 参数列表
     */
    List<InterfaceParameter> selectByInterfaceId(@Param("interfaceId") String interfaceId);

    /**
     * 根据接口ID删除参数
     *
     * @param interfaceId 接口ID
     * @return 删除数量
     */
    int deleteByInterfaceId(@Param("interfaceId") String interfaceId);

    /**
     * 批量插入参数
     *
     * @param parameters 参数列表
     * @return 插入数量
     */
    int batchInsert(@Param("parameters") List<InterfaceParameter> parameters);

    /**
     * 根据接口ID和参数名查询参数
     *
     * @param interfaceId 接口ID
     * @param paramName 参数名
     * @return 参数信息
     */
    InterfaceParameter selectByInterfaceIdAndParamName(@Param("interfaceId") String interfaceId, 
                                                      @Param("paramName") String paramName);

    /**
     * 获取接口的必需参数列表
     *
     * @param interfaceId 接口ID
     * @return 必需参数列表
     */
    List<InterfaceParameter> selectRequiredParameters(@Param("interfaceId") String interfaceId);

    /**
     * 获取标准参数模板
     *
     * @return 标准参数列表
     */
    List<InterfaceParameter> selectStandardParameters();
}