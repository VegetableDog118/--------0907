package com.powertrading.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段信息DTO
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfo {
    /**
     * 字段名
     */
    private String columnName;
    
    /**
     * 数据类型
     */
    private String dataType;
    
    /**
     * 字段说明
     */
    private String columnComment;
    
    /**
     * 是否为主键
     */
    private Boolean isPrimaryKey;
    
    /**
     * 是否可为空
     */
    private Boolean isNullable;
}