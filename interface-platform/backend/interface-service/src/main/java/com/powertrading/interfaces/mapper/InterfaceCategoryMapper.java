package com.powertrading.interfaces.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powertrading.interfaces.entity.InterfaceCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 接口分类Mapper接口
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Mapper
public interface InterfaceCategoryMapper extends BaseMapper<InterfaceCategory> {

    /**
     * 查询所有启用的分类
     *
     * @return 分类列表
     */
    List<InterfaceCategory> selectEnabledCategories();

    /**
     * 根据分类编码查询分类
     *
     * @param categoryCode 分类编码
     * @return 分类信息
     */
    InterfaceCategory selectByCategoryCode(@Param("categoryCode") String categoryCode);

    /**
     * 根据分类名称查询分类
     *
     * @param categoryName 分类名称
     * @return 分类信息
     */
    InterfaceCategory selectByCategoryName(@Param("categoryName") String categoryName);

    /**
     * 查询分类及其接口数量
     *
     * @return 分类统计列表
     */
    List<CategoryStatistics> selectCategoryStatistics();

    /**
     * 分类统计信息内部类
     */
    class CategoryStatistics {
        private String id;
        private String categoryCode;
        private String categoryName;
        private String description;
        private String color;
        private Integer sortOrder;
        private Long interfaceCount;
        private Long publishedCount;
        
        // getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCategoryCode() { return categoryCode; }
        public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public Long getInterfaceCount() { return interfaceCount; }
        public void setInterfaceCount(Long interfaceCount) { this.interfaceCount = interfaceCount; }
        public Long getPublishedCount() { return publishedCount; }
        public void setPublishedCount(Long publishedCount) { this.publishedCount = publishedCount; }
    }
}