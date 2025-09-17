package com.powertrading.interfaces.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.powertrading.interfaces.entity.InterfaceCategory;
import com.powertrading.interfaces.mapper.InterfaceCategoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 接口分类管理服务
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class InterfaceCategoryService {

    private static final Logger log = LoggerFactory.getLogger(InterfaceCategoryService.class);

    @Autowired
    private InterfaceCategoryMapper categoryMapper;

    /**
     * 获取所有启用的分类
     *
     * @return 分类列表
     */
    @Cacheable(value = "interface:categories")
    public List<InterfaceCategory> getEnabledCategories() {
        try {
            return categoryMapper.selectEnabledCategories();
        } catch (Exception e) {
            log.error("获取启用分类列表失败", e);
            throw new RuntimeException("获取启用分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有分类（包括禁用的）
     *
     * @return 分类列表
     */
    public List<InterfaceCategory> getAllCategories() {
        try {
            QueryWrapper<InterfaceCategory> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByAsc("sort_order", "create_time");
            return categoryMapper.selectList(queryWrapper);
        } catch (Exception e) {
            log.error("获取所有分类列表失败", e);
            throw new RuntimeException("获取所有分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取分类详情
     *
     * @param categoryId 分类ID
     * @return 分类详情
     */
    @Cacheable(value = "interface:category", key = "#categoryId")
    public InterfaceCategory getCategoryById(String categoryId) {
        try {
            InterfaceCategory category = categoryMapper.selectById(categoryId);
            if (category == null) {
                throw new RuntimeException("分类不存在");
            }
            return category;
        } catch (Exception e) {
            log.error("获取分类详情失败，分类ID: {}", categoryId, e);
            throw new RuntimeException("获取分类详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建分类
     *
     * @param request 创建请求
     * @param createBy 创建人
     * @return 分类ID
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "interface:categories", allEntries = true)
    public String createCategory(CategoryCreateRequest request, String createBy) {
        try {
            // 验证分类编码是否重复
            InterfaceCategory existingByCode = categoryMapper.selectByCategoryCode(request.getCategoryCode());
            if (existingByCode != null) {
                throw new RuntimeException("分类编码已存在");
            }
            
            // 验证分类名称是否重复
            InterfaceCategory existingByName = categoryMapper.selectByCategoryName(request.getCategoryName());
            if (existingByName != null) {
                throw new RuntimeException("分类名称已存在");
            }
            
            // 创建分类
            InterfaceCategory category = new InterfaceCategory();
            String categoryId = UUID.randomUUID().toString().replace("-", "");
            category.setId(categoryId);
            category.setCategoryCode(request.getCategoryCode());
            category.setCategoryName(request.getCategoryName());
            category.setDescription(request.getDescription());
            category.setStatus(InterfaceCategory.STATUS_ENABLED);
            category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
            category.setCreateBy(createBy);
            
            categoryMapper.insert(category);
            
            log.info("分类创建成功，分类ID: {}, 分类编码: {}, 创建人: {}", 
                categoryId, request.getCategoryCode(), createBy);
            
            return categoryId;
            
        } catch (Exception e) {
            log.error("创建分类失败，分类编码: {}", request.getCategoryCode(), e);
            throw new RuntimeException("创建分类失败: " + e.getMessage());
        }
    }

    /**
     * 更新分类
     *
     * @param categoryId 分类ID
     * @param request 更新请求
     * @param updateBy 更新人
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:categories", "interface:category"}, allEntries = true)
    public void updateCategory(String categoryId, CategoryUpdateRequest request, String updateBy) {
        try {
            // 检查分类是否存在
            InterfaceCategory existingCategory = categoryMapper.selectById(categoryId);
            if (existingCategory == null) {
                throw new RuntimeException("分类不存在");
            }
            
            // 验证分类编码是否重复（排除自己）
            if (StringUtils.hasText(request.getCategoryCode()) && 
                !request.getCategoryCode().equals(existingCategory.getCategoryCode())) {
                InterfaceCategory existingByCode = categoryMapper.selectByCategoryCode(request.getCategoryCode());
                if (existingByCode != null) {
                    throw new RuntimeException("分类编码已存在");
                }
            }
            
            // 验证分类名称是否重复（排除自己）
            if (StringUtils.hasText(request.getCategoryName()) && 
                !request.getCategoryName().equals(existingCategory.getCategoryName())) {
                InterfaceCategory existingByName = categoryMapper.selectByCategoryName(request.getCategoryName());
                if (existingByName != null) {
                    throw new RuntimeException("分类名称已存在");
                }
            }
            
            // 更新分类
            InterfaceCategory updateCategory = new InterfaceCategory();
            updateCategory.setId(categoryId);
            updateCategory.setUpdateBy(updateBy);
            
            if (StringUtils.hasText(request.getCategoryCode())) {
                updateCategory.setCategoryCode(request.getCategoryCode());
            }
            
            if (StringUtils.hasText(request.getCategoryName())) {
                updateCategory.setCategoryName(request.getCategoryName());
            }
            
            if (StringUtils.hasText(request.getDescription())) {
                updateCategory.setDescription(request.getDescription());
            }
            
            if (request.getSortOrder() != null) {
                updateCategory.setSortOrder(request.getSortOrder());
            }
            
            categoryMapper.updateById(updateCategory);
            
            log.info("分类更新成功，分类ID: {}, 更新人: {}", categoryId, updateBy);
            
        } catch (Exception e) {
            log.error("更新分类失败，分类ID: {}", categoryId, e);
            throw new RuntimeException("更新分类失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用分类
     *
     * @param categoryId 分类ID
     * @param status 状态
     * @param updateBy 更新人
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:categories", "interface:category"}, allEntries = true)
    public void updateCategoryStatus(String categoryId, String status, String updateBy) {
        try {
            // 检查分类是否存在
            InterfaceCategory existingCategory = categoryMapper.selectById(categoryId);
            if (existingCategory == null) {
                throw new RuntimeException("分类不存在");
            }
            
            // 检查是否为预定义分类
            if (InterfaceCategory.CATEGORY_BASIC_DATA.equals(existingCategory.getCategoryCode()) ||
                InterfaceCategory.CATEGORY_BUSINESS_DATA.equals(existingCategory.getCategoryCode()) ||
                InterfaceCategory.CATEGORY_STATISTICAL_DATA.equals(existingCategory.getCategoryCode())) {
                throw new RuntimeException("预定义分类不能禁用");
            }
            
            // 如果要禁用分类，检查是否有接口在使用
            if (InterfaceCategory.STATUS_DISABLED.equals(status)) {
                // 这里应该检查是否有接口使用了该分类
                // 暂时跳过检查
            }
            
            // 更新状态
            InterfaceCategory updateCategory = new InterfaceCategory();
            updateCategory.setId(categoryId);
            updateCategory.setStatus(Integer.valueOf(status));
            updateCategory.setUpdateBy(updateBy);
            
            categoryMapper.updateById(updateCategory);
            
            log.info("分类状态更新成功，分类ID: {}, 状态: {}, 更新人: {}", 
                categoryId, status, updateBy);
            
        } catch (Exception e) {
            log.error("更新分类状态失败，分类ID: {}, 状态: {}", categoryId, status, e);
            throw new RuntimeException("更新分类状态失败: " + e.getMessage());
        }
    }

    /**
     * 删除分类
     *
     * @param categoryId 分类ID
     * @param deleteBy 删除人
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:categories", "interface:category"}, allEntries = true)
    public void deleteCategory(String categoryId, String deleteBy) {
        try {
            // 检查分类是否存在
            InterfaceCategory existingCategory = categoryMapper.selectById(categoryId);
            if (existingCategory == null) {
                throw new RuntimeException("分类不存在");
            }
            
            // 检查是否为预定义分类
            if (InterfaceCategory.CATEGORY_BASIC_DATA.equals(existingCategory.getCategoryCode()) ||
                InterfaceCategory.CATEGORY_BUSINESS_DATA.equals(existingCategory.getCategoryCode()) ||
                InterfaceCategory.CATEGORY_STATISTICAL_DATA.equals(existingCategory.getCategoryCode())) {
                throw new RuntimeException("预定义分类不能删除");
            }
            
            // 检查是否有接口在使用该分类
            // 这里应该检查是否有接口使用了该分类
            // 暂时跳过检查
            
            // 删除分类
            categoryMapper.deleteById(categoryId);
            
            log.info("分类删除成功，分类ID: {}, 删除人: {}", categoryId, deleteBy);
            
        } catch (Exception e) {
            log.error("删除分类失败，分类ID: {}", categoryId, e);
            throw new RuntimeException("删除分类失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类统计信息
     *
     * @return 统计信息列表
     */
    public List<InterfaceCategoryMapper.CategoryStatistics> getCategoryStatistics() {
        try {
            return categoryMapper.selectCategoryStatistics();
        } catch (Exception e) {
            log.error("获取分类统计信息失败", e);
            throw new RuntimeException("获取分类统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 初始化预定义分类
     */
    @Transactional(rollbackFor = Exception.class)
    public void initPredefinedCategories() {
        try {
            // 基础数据类
            createPredefinedCategory(
                InterfaceCategory.CATEGORY_BASIC_DATA,
                "基础数据类",
                "提供基础数据查询服务，如字典数据、配置信息等",
                1
            );
            
            // 业务数据类
            createPredefinedCategory(
                InterfaceCategory.CATEGORY_BUSINESS_DATA,
                "业务数据类",
                "提供核心业务数据查询服务，如交易数据、客户信息等",
                2
            );
            
            // 统计数据类
            createPredefinedCategory(
                InterfaceCategory.CATEGORY_STATISTICAL_DATA,
                "统计数据类",
                "提供统计分析数据查询服务，如报表数据、汇总信息等",
                3
            );
            
            log.info("预定义分类初始化完成");
            
        } catch (Exception e) {
            log.error("初始化预定义分类失败", e);
            throw new RuntimeException("初始化预定义分类失败: " + e.getMessage());
        }
    }

    /**
     * 创建预定义分类
     */
    private void createPredefinedCategory(String categoryCode, String categoryName, 
                                        String description, Integer sortOrder) {
        // 检查是否已存在
        InterfaceCategory existing = categoryMapper.selectByCategoryCode(categoryCode);
        if (existing != null) {
            return; // 已存在，跳过
        }
        
        InterfaceCategory category = new InterfaceCategory();
        category.setId(UUID.randomUUID().toString().replace("-", ""));
        category.setCategoryCode(categoryCode);
        category.setCategoryName(categoryName);
        category.setDescription(description);
        category.setStatus(InterfaceCategory.STATUS_ENABLED);
        category.setSortOrder(sortOrder);
        category.setCreateBy("system");
        
        categoryMapper.insert(category);
    }

    /**
     * 分类创建请求
     */
    public static class CategoryCreateRequest {
        private String categoryCode;
        private String categoryName;
        private String description;
        private Integer sortOrder;
        
        // getters and setters
        public String getCategoryCode() { return categoryCode; }
        public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    /**
     * 分类更新请求
     */
    public static class CategoryUpdateRequest {
        private String categoryCode;
        private String categoryName;
        private String description;
        private Integer sortOrder;
        
        // getters and setters
        public String getCategoryCode() { return categoryCode; }
        public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}