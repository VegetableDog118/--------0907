package com.powertrading.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MySQL表信息DTO
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MysqlTableInfo {
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 表说明
     */
    private String tableComment;
    
    /**
     * 表类型
     */
    private String tableType;
    
    /**
     * 记录数
     */
    private Long recordCount;
}