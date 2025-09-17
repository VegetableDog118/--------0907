package com.powertrading.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 表结构信息DTO
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableStructureInfo {
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 字段列表
     */
    private List<ColumnInfo> columns;
}