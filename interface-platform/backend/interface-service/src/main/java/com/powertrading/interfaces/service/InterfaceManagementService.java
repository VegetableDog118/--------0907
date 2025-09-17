package com.powertrading.interfaces.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.interfaces.client.GatewayClient;
import com.powertrading.interfaces.dto.InterfaceQueryRequest;
import com.powertrading.interfaces.entity.Interface;
import com.powertrading.interfaces.entity.InterfaceCategory;
import com.powertrading.interfaces.entity.InterfaceParameter;
import com.powertrading.interfaces.mapper.InterfaceCategoryMapper;
import com.powertrading.interfaces.mapper.InterfaceMapper;
import com.powertrading.interfaces.mapper.InterfaceMapperImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.powertrading.interfaces.mapper.InterfaceParameterMapper;
import com.powertrading.interfaces.vo.InterfaceVO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 接口管理服务
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class InterfaceManagementService {

    private static final Logger log = LoggerFactory.getLogger(InterfaceManagementService.class);

    @Autowired
    private InterfaceMapper interfaceMapper;
    
    @Autowired
    private InterfaceMapperImpl interfaceMapperImpl;

    @Autowired
    private InterfaceParameterMapper parameterMapper;

    @Autowired
    private InterfaceCategoryMapper categoryMapper;

    @Autowired
    private GatewayClient gatewayClient;

    /**
     * 分页查询接口列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @Cacheable(value = "interface:list", key = "#request.hashCode()", unless = "#result.records.size() == 0")
    public IPage<InterfaceVO> getInterfaceList(InterfaceQueryRequest request) {
        try {
            Page<InterfaceVO> page = new Page<>(request.getPage(), request.getSize());
            // 使用新的InterfaceMapperImpl避免XML映射的参数绑定问题
            IPage<InterfaceVO> result = interfaceMapperImpl.selectInterfacePageNew(page, request);
            
            // 填充额外信息
            if (!CollectionUtils.isEmpty(result.getRecords())) {
                fillInterfaceExtraInfo(result.getRecords());
            }
            
            return result;
        } catch (Exception e) {
            log.error("查询接口列表失败", e);
            throw new RuntimeException("查询接口列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取接口详情
     *
     * @param interfaceId 接口ID
     * @return 接口详情
     */
    @Cacheable(value = "interface:detail", key = "#interfaceId")
    public InterfaceVO getInterfaceDetail(String interfaceId) {
        try {
            // 使用新的InterfaceMapperImpl避免XML映射的参数绑定问题
            InterfaceVO interfaceVO = interfaceMapperImpl.selectInterfaceByIdNew(interfaceId);
            if (interfaceVO == null) {
                throw new RuntimeException("接口不存在");
            }
            
            // 获取参数列表
            List<InterfaceParameter> parameters = parameterMapper.selectByInterfaceId(interfaceId);
            if (!CollectionUtils.isEmpty(parameters)) {
                List<InterfaceVO.ParameterInfo> parameterInfos = parameters.stream()
                    .map(this::convertToParameterInfo)
                    .sorted(Comparator.comparing(InterfaceVO.ParameterInfo::getSortOrder))
                    .collect(Collectors.toList());
                interfaceVO.setParameters(parameterInfos);
            }
            
            return interfaceVO;
        } catch (Exception e) {
            log.error("获取接口详情失败，接口ID: {}", interfaceId, e);
            throw new RuntimeException("获取接口详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新接口配置
     *
     * @param interfaceId 接口ID
     * @param request 更新请求
     * @param updateBy 更新人
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:detail", "interface:list"}, allEntries = true)
    public void updateInterfaceConfig(String interfaceId, InterfaceUpdateRequest request, String updateBy) {
        try {
            // 检查接口是否存在
            Interface existingInterface = interfaceMapper.selectById(interfaceId);
            if (existingInterface == null) {
                throw new RuntimeException("接口不存在");
            }
            
            // 检查接口状态（已上架的接口不能修改核心配置）
            if (Interface.STATUS_PUBLISHED.equals(existingInterface.getStatus()) && 
                (StringUtils.hasText(request.getInterfaceName()) || 
                 request.getCategoryId() != null)) {
                throw new RuntimeException("已上架的接口不能修改名称或分类");
            }
            
            // 更新接口基本信息
            Interface updateInterface = new Interface();
            updateInterface.setId(interfaceId);
            updateInterface.setUpdateBy(updateBy);
            
            if (StringUtils.hasText(request.getInterfaceName())) {
                // 检查名称是否重复 - 使用MyBatis-Plus查询避免参数绑定问题
                QueryWrapper<Interface> nameQuery = new QueryWrapper<>();
                nameQuery.eq("interface_name", request.getInterfaceName())
                         .ne("id", interfaceId);
                Interface existingByName = interfaceMapper.selectOne(nameQuery);
                if (existingByName != null) {
                    throw new RuntimeException("接口名称已存在");
                }
                updateInterface.setInterfaceName(request.getInterfaceName());
            }
            
            if (StringUtils.hasText(request.getDescription())) {
                updateInterface.setDescription(request.getDescription());
            }
            
            if (request.getCategoryId() != null) {
                updateInterface.setCategoryId(request.getCategoryId());
            }
            
            if (request.getRateLimit() != null) {
                updateInterface.setRateLimit(request.getRateLimit());
            }
            
            if (request.getTimeout() != null) {
                updateInterface.setTimeout(request.getTimeout());
            }
            
            interfaceMapper.updateById(updateInterface);
            
            log.info("接口配置更新成功，接口ID: {}, 更新人: {}", interfaceId, updateBy);
            
        } catch (Exception e) {
            log.error("更新接口配置失败，接口ID: {}", interfaceId, e);
            throw new RuntimeException("更新接口配置失败: " + e.getMessage());
        }
    }

    /**
     * 更新接口参数
     *
     * @param interfaceId 接口ID
     * @param parameters 参数列表
     * @param updateBy 更新人
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:detail", "interface:list"}, allEntries = true)
    public void updateInterfaceParameters(String interfaceId, List<InterfaceParameterRequest> parameters, String updateBy) {
        try {
            // 检查接口是否存在
            Interface existingInterface = interfaceMapper.selectById(interfaceId);
            if (existingInterface == null) {
                throw new RuntimeException("接口不存在");
            }
            
            // 检查接口状态（已上架的接口不能修改参数）
            if (Interface.STATUS_PUBLISHED.equals(existingInterface.getStatus())) {
                throw new RuntimeException("已上架的接口不能修改参数配置");
            }
            
            // 删除原有参数
            parameterMapper.deleteByInterfaceId(interfaceId);
            
            // 添加新参数
            if (!CollectionUtils.isEmpty(parameters)) {
                List<InterfaceParameter> parameterEntities = new ArrayList<>();
                for (int i = 0; i < parameters.size(); i++) {
                    InterfaceParameterRequest paramRequest = parameters.get(i);
                    
                    InterfaceParameter parameter = new InterfaceParameter();
                    parameter.setId(UUID.randomUUID().toString().replace("-", ""));
                    parameter.setInterfaceId(interfaceId);
                    parameter.setParamName(paramRequest.getParamName());
                    parameter.setParamType(paramRequest.getParamType());
                    parameter.setParamLocation(paramRequest.getParamLocation());
                    parameter.setDescription(paramRequest.getDescription());
                    parameter.setRequired(paramRequest.getRequired());
                    parameter.setDefaultValue(paramRequest.getDefaultValue());
                    parameter.setValidationRule(paramRequest.getValidationRule());
                    parameter.setExample(paramRequest.getExample());
                    parameter.setSortOrder(paramRequest.getSortOrder() != null ? paramRequest.getSortOrder() : i);
                    
                    parameterEntities.add(parameter);
                }
                
                parameterMapper.batchInsert(parameterEntities);
            }
            
            // 更新接口的更新时间和更新人
            Interface updateInterface = new Interface();
            updateInterface.setId(interfaceId);
            updateInterface.setUpdateBy(updateBy);
            interfaceMapper.updateById(updateInterface);
            
            log.info("接口参数更新成功，接口ID: {}, 参数数量: {}, 更新人: {}", 
                interfaceId, parameters.size(), updateBy);
            
        } catch (Exception e) {
            log.error("更新接口参数失败，接口ID: {}", interfaceId, e);
            throw new RuntimeException("更新接口参数失败: " + e.getMessage());
        }
    }

    /**
     * 删除接口
     *
     * @param interfaceId 接口ID
     * @param deleteBy 删除人
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:detail", "interface:list"}, allEntries = true)
    public void deleteInterface(String interfaceId, String deleteBy) {
        try {
            // 检查接口是否存在
            Interface existingInterface = interfaceMapper.selectById(interfaceId);
            if (existingInterface == null) {
                throw new RuntimeException("接口不存在");
            }
            
            // 检查接口状态（已上架的接口不能删除）
            if (Interface.STATUS_PUBLISHED.equals(existingInterface.getStatus())) {
                throw new RuntimeException("已上架的接口不能删除，请先下架");
            }
            
            // 删除接口参数
            parameterMapper.deleteByInterfaceId(interfaceId);
            
            // 删除接口
            interfaceMapper.deleteById(interfaceId);
            
            log.info("接口删除成功，接口ID: {}, 删除人: {}", interfaceId, deleteBy);
            
        } catch (Exception e) {
            log.error("删除接口失败，接口ID: {}", interfaceId, e);
            throw new RuntimeException("删除接口失败: " + e.getMessage());
        }
    }

    /**
     * 获取接口分类列表
     *
     * @return 分类列表
     */
    @Cacheable(value = "interface:categories")
    public List<InterfaceCategory> getInterfaceCategories() {
        try {
            return categoryMapper.selectEnabledCategories();
        } catch (Exception e) {
            log.error("获取接口分类列表失败", e);
            throw new RuntimeException("获取接口分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取标准参数模板
     *
     * @return 标准参数列表
     */
    @Cacheable(value = "interface:standard-parameters")
    public List<InterfaceParameter> getStandardParameterTemplates() {
        try {
            List<InterfaceParameter> standardParams = new ArrayList<>();
            
            // dataTime参数
            InterfaceParameter dataTimeParam = new InterfaceParameter();
            dataTimeParam.setParamName("dataTime");
            dataTimeParam.setParamType("string");
            dataTimeParam.setParamLocation("body");
            dataTimeParam.setDescription("查询日期，格式：YYYY-MM-DD");
            dataTimeParam.setRequired(true);
            dataTimeParam.setValidationRule("date:YYYY-MM-DD,max:yesterday");
            dataTimeParam.setExample("2022-03-17");
            dataTimeParam.setSortOrder(1);
            standardParams.add(dataTimeParam);
            
            // appId参数
            InterfaceParameter appIdParam = new InterfaceParameter();
            appIdParam.setParamName("appId");
            appIdParam.setParamType("string");
            appIdParam.setParamLocation("body");
            appIdParam.setDescription("应用ID，用户身份标识");
            appIdParam.setRequired(true);
            appIdParam.setValidationRule("string,length:15-20");
            appIdParam.setExample("KzoHypQZH4-F6qM63L");
            appIdParam.setSortOrder(2);
            standardParams.add(appIdParam);
            
            return standardParams;
        } catch (Exception e) {
            log.error("获取标准参数模板失败", e);
            throw new RuntimeException("获取标准参数模板失败: " + e.getMessage());
        }
    }

    /**
     * 复制接口
     *
     * @param sourceInterfaceId 源接口ID
     * @param newInterfaceName 新接口名称
     * @param createBy 创建人
     * @return 新接口ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String copyInterface(String sourceInterfaceId, String newInterfaceName, String createBy) {
        try {
            // 获取源接口信息
            Interface sourceInterface = interfaceMapper.selectById(sourceInterfaceId);
            if (sourceInterface == null) {
                throw new RuntimeException("源接口不存在");
            }
            
            // 检查新接口名称是否重复 - 使用MyBatis-Plus查询避免参数绑定问题
            QueryWrapper<Interface> nameQuery = new QueryWrapper<>();
            nameQuery.eq("interface_name", newInterfaceName);
            Interface existingByName = interfaceMapper.selectOne(nameQuery);
            if (existingByName != null) {
                throw new RuntimeException("接口名称已存在");
            }
            
            // 创建新接口
            Interface newInterface = new Interface();
            String newInterfaceId = UUID.randomUUID().toString().replace("-", "");
            newInterface.setId(newInterfaceId);
            newInterface.setInterfaceName(newInterfaceName);
            newInterface.setInterfacePath(sourceInterface.getInterfacePath() + "_copy");
            newInterface.setDescription(sourceInterface.getDescription() + "（复制）");
            newInterface.setCategoryId(sourceInterface.getCategoryId());
            newInterface.setDataSourceId(sourceInterface.getDataSourceId());
            newInterface.setTableName(sourceInterface.getTableName());
            newInterface.setRequestMethod(sourceInterface.getRequestMethod());
            newInterface.setStatus(Interface.STATUS_UNPUBLISHED);
            newInterface.setVersion("1.0");
            newInterface.setSqlTemplate(sourceInterface.getSqlTemplate());
            newInterface.setResponseFormat(sourceInterface.getResponseFormat());
            newInterface.setRateLimit(sourceInterface.getRateLimit());
            newInterface.setTimeout(sourceInterface.getTimeout());
            newInterface.setCreateBy(createBy);
            
            interfaceMapper.insert(newInterface);
            
            // 复制参数
            List<InterfaceParameter> sourceParameters = parameterMapper.selectByInterfaceId(sourceInterfaceId);
            if (!CollectionUtils.isEmpty(sourceParameters)) {
                List<InterfaceParameter> newParameters = sourceParameters.stream()
                    .map(param -> {
                        InterfaceParameter newParam = new InterfaceParameter();
                        newParam.setId(UUID.randomUUID().toString().replace("-", ""));
                        newParam.setInterfaceId(newInterfaceId);
                        newParam.setParamName(param.getParamName());
                        newParam.setParamType(param.getParamType());
                        newParam.setParamLocation(param.getParamLocation());
                        newParam.setDescription(param.getDescription());
                        newParam.setRequired(param.getRequired());
                        newParam.setDefaultValue(param.getDefaultValue());
                        newParam.setValidationRule(param.getValidationRule());
                        newParam.setExample(param.getExample());
                        newParam.setSortOrder(param.getSortOrder());
                        return newParam;
                    })
                    .collect(Collectors.toList());
                
                parameterMapper.batchInsert(newParameters);
            }
            
            log.info("接口复制成功，源接口ID: {}, 新接口ID: {}, 创建人: {}", 
                sourceInterfaceId, newInterfaceId, createBy);
            
            return newInterfaceId;
            
        } catch (Exception e) {
            log.error("复制接口失败，源接口ID: {}", sourceInterfaceId, e);
            throw new RuntimeException("复制接口失败: " + e.getMessage());
        }
    }

    /**
     * 填充接口额外信息
     */
    private void fillInterfaceExtraInfo(List<InterfaceVO> interfaces) {
        for (InterfaceVO interfaceVO : interfaces) {
            // 设置状态显示名称
            interfaceVO.setStatusDisplay(getStatusDisplayName(interfaceVO.getStatus()));
        }
    }

    /**
     * 获取状态显示名称
     */
    private String getStatusDisplayName(String status) {
        switch (status) {
            case Interface.STATUS_UNPUBLISHED:
                return "未上架";
            case Interface.STATUS_PUBLISHED:
                return "已上架";
            case Interface.STATUS_OFFLINE:
                return "已下架";
            default:
                return status;
        }
    }

    /**
     * 转换参数信息
     */
    private InterfaceVO.ParameterInfo convertToParameterInfo(InterfaceParameter parameter) {
        InterfaceVO.ParameterInfo info = new InterfaceVO.ParameterInfo();
        info.setId(parameter.getId());
        info.setParamName(parameter.getParamName());
        info.setParamType(parameter.getParamType());
        info.setParamLocation(parameter.getParamLocation());
        info.setDescription(parameter.getDescription());
        info.setRequired(parameter.getRequired());
        info.setDefaultValue(parameter.getDefaultValue());
        info.setValidationRule(parameter.getValidationRule());
        info.setExample(parameter.getExample());
        info.setSortOrder(parameter.getSortOrder());
        return info;
    }

    /**
     * 接口更新请求
     */
    public static class InterfaceUpdateRequest {
        private String interfaceName;
        private String description;
        private String categoryId;
        private Integer rateLimit;
        private Integer timeout;
        
        // getters and setters
        public String getInterfaceName() { return interfaceName; }
        public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategoryId() { return categoryId; }
        public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
        public Integer getRateLimit() { return rateLimit; }
        public void setRateLimit(Integer rateLimit) { this.rateLimit = rateLimit; }
        public Integer getTimeout() { return timeout; }
        public void setTimeout(Integer timeout) { this.timeout = timeout; }
    }

    /**
     * 接口参数请求
     */
    public static class InterfaceParameterRequest {
        private String paramName;
        private String paramType;
        private String paramLocation;
        private String description;
        private Boolean required;
        private String defaultValue;
        private String validationRule;
        private String example;
        private Integer sortOrder;
        
        // getters and setters
        public String getParamName() { return paramName; }
        public void setParamName(String paramName) { this.paramName = paramName; }
        public String getParamType() { return paramType; }
        public void setParamType(String paramType) { this.paramType = paramType; }
        public String getParamLocation() { return paramLocation; }
        public void setParamLocation(String paramLocation) { this.paramLocation = paramLocation; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getRequired() { return required; }
        public void setRequired(Boolean required) { this.required = required; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        public String getValidationRule() { return validationRule; }
        public void setValidationRule(String validationRule) { this.validationRule = validationRule; }
        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}