package com.powertrading.interfaces.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.interfaces.dto.InterfaceQueryRequest;
import com.powertrading.interfaces.entity.Interface;
import com.powertrading.interfaces.vo.InterfaceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InterfaceMapper的MyBatis-Plus实现
 * 用于替代XML映射文件中的复杂查询
 */
@Component
public class InterfaceMapperImpl {

    @Autowired
    private InterfaceMapper interfaceMapper;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 分页查询接口列表（带关联信息）
     * 替代XML中的selectInterfacePage方法
     */
    public IPage<InterfaceVO> selectInterfacePageNew(Page<InterfaceVO> page, InterfaceQueryRequest request) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("    i.id, i.interface_name, i.interface_path, i.description, i.category_id, ");
        sql.append("    i.status, i.version, i.create_time, i.update_time, i.publish_time, i.create_by, ");
        sql.append("    ic.category_name, ");
        sql.append("    u.real_name as creator_name ");
        sql.append("FROM interfaces i ");
        sql.append("LEFT JOIN interface_categories ic ON i.category_id = ic.id ");
        sql.append("LEFT JOIN users u ON i.create_by = u.id ");
        sql.append("WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        // 动态条件构建
        if (request != null) {
            if (!CollectionUtils.isEmpty(request.getCategoryIds())) {
                sql.append("AND i.category_id IN (");
                for (int i = 0; i < request.getCategoryIds().size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("?");
                    params.add(request.getCategoryIds().get(i));
                }
                sql.append(") ");
            }
            
            if (!CollectionUtils.isEmpty(request.getStatusList())) {
                sql.append("AND i.status IN (");
                for (int i = 0; i < request.getStatusList().size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("?");
                    params.add(request.getStatusList().get(i));
                }
                sql.append(") ");
            }
            
            if (StringUtils.hasText(request.getKeyword())) {
                sql.append("AND (");
                sql.append("    i.interface_name LIKE CONCAT('%', ?, '%') ");
                sql.append("    OR i.description LIKE CONCAT('%', ?, '%') ");
                sql.append("    OR i.interface_path LIKE CONCAT('%', ?, '%') ");
                sql.append(") ");
                params.add(request.getKeyword());
                params.add(request.getKeyword());
                params.add(request.getKeyword());
            }
            
            if (request.getPublishedOnly() != null && request.getPublishedOnly()) {
                sql.append("AND i.status = ? ");
                params.add("published");
            }
        }
        
        // 排序
        sql.append("ORDER BY ");
        sql.append("    CASE WHEN i.status = 'published' THEN 1 ");
        sql.append("         WHEN i.status = 'unpublished' THEN 2 ");
        sql.append("         ELSE 3 END, ");
        sql.append("    i.update_time DESC ");
        
        // 分页
        sql.append("LIMIT ?, ?");
        params.add((page.getCurrent() - 1) * page.getSize());
        params.add(page.getSize());
        
        // 执行查询
        List<InterfaceVO> records = jdbcTemplate.query(sql.toString(), params.toArray(), 
            new BeanPropertyRowMapper<>(InterfaceVO.class));
        
        // 查询总数
        String countSql = buildCountSql(request);
        List<Object> countParams = buildCountParams(request);
        Long total = jdbcTemplate.queryForObject(countSql, countParams.toArray(), Long.class);
        
        // 设置分页结果
        page.setRecords(records);
        page.setTotal(total);
        
        return page;
    }
    
    /**
     * 根据ID查询接口详情（带关联信息）
     * 替代XML中的selectInterfaceById方法
     */
    public InterfaceVO selectInterfaceByIdNew(String id) {
        String sql = "SELECT " +
                "    i.id, i.interface_name, i.interface_path, i.description, i.category_id, " +
                "    i.status, i.version, i.create_time, i.update_time, i.publish_time, i.create_by, " +
                "    ic.category_name, " +
                "    u.real_name as creator_name " +
                "FROM interfaces i " +
                "LEFT JOIN interface_categories ic ON i.category_id = ic.id " +
                "LEFT JOIN users u ON i.create_by = u.id " +
                "WHERE i.id = ?";
        
        List<InterfaceVO> results = jdbcTemplate.query(sql, new Object[]{id}, 
            new BeanPropertyRowMapper<>(InterfaceVO.class));
        
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * 批量更新接口状态
     * 替代XML中的batchUpdateStatus方法
     */
    public int batchUpdateStatusNew(List<String> ids, String status, String operatorId) {
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE interfaces SET ");
        sql.append("    status = ?, ");
        sql.append("    update_by = ?, ");
        sql.append("    update_time = NOW() ");
        sql.append("WHERE id IN (");
        
        List<Object> params = new ArrayList<>();
        params.add(status);
        params.add(operatorId);
        
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
            params.add(ids.get(i));
        }
        sql.append(")");
        
        return jdbcTemplate.update(sql.toString(), params.toArray());
    }
    
    /**
     * 查询接口统计信息
     * 替代XML中的selectInterfaceStatistics方法
     */
    public List<InterfaceMapper.InterfaceStatistics> selectInterfaceStatisticsNew() {
        String sql = "SELECT status, COUNT(*) as count FROM interfaces GROUP BY status";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            InterfaceMapper.InterfaceStatistics stats = new InterfaceMapper.InterfaceStatistics();
            stats.setStatus(rs.getString("status"));
            stats.setCount(rs.getLong("count"));
            return stats;
        });
    }
    
    /**
     * 查询已发布的接口列表
     * 替代XML中的selectPublishedInterfaces方法
     */
    public List<InterfaceVO> selectPublishedInterfacesNew() {
        String sql = "SELECT " +
                "    i.id, i.interface_name, i.interface_path, i.description, i.category_id, " +
                "    i.status, i.version, i.create_time, i.update_time, i.publish_time, i.create_by, " +
                "    ic.category_name, " +
                "    u.real_name as creator_name " +
                "FROM interfaces i " +
                "LEFT JOIN interface_categories ic ON i.category_id = ic.id " +
                "LEFT JOIN users u ON i.create_by = u.id " +
                "WHERE i.status = 'published' " +
                "ORDER BY i.publish_time DESC";
        
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(InterfaceVO.class));
    }
    
    // 辅助方法：构建计数SQL
    private String buildCountSql(InterfaceQueryRequest request) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM interfaces i WHERE 1=1 ");
        
        if (request != null) {
            if (!CollectionUtils.isEmpty(request.getCategoryIds())) {
                sql.append("AND i.category_id IN (");
                for (int i = 0; i < request.getCategoryIds().size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("?");
                }
                sql.append(") ");
            }
            
            if (!CollectionUtils.isEmpty(request.getStatusList())) {
                sql.append("AND i.status IN (");
                for (int i = 0; i < request.getStatusList().size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("?");
                }
                sql.append(") ");
            }
            
            if (StringUtils.hasText(request.getKeyword())) {
                sql.append("AND (");
                sql.append("    i.interface_name LIKE CONCAT('%', ?, '%') ");
                sql.append("    OR i.description LIKE CONCAT('%', ?, '%') ");
                sql.append("    OR i.interface_path LIKE CONCAT('%', ?, '%') ");
                sql.append(") ");
            }
            
            if (request.getPublishedOnly() != null && request.getPublishedOnly()) {
                sql.append("AND i.status = ? ");
            }
        }
        
        return sql.toString();
    }
    
    // 辅助方法：构建计数参数
    private List<Object> buildCountParams(InterfaceQueryRequest request) {
        List<Object> params = new ArrayList<>();
        
        if (request != null) {
            if (!CollectionUtils.isEmpty(request.getCategoryIds())) {
                params.addAll(request.getCategoryIds());
            }
            
            if (!CollectionUtils.isEmpty(request.getStatusList())) {
                params.addAll(request.getStatusList());
            }
            
            if (StringUtils.hasText(request.getKeyword())) {
                params.add(request.getKeyword());
                params.add(request.getKeyword());
                params.add(request.getKeyword());
            }
            
            if (request.getPublishedOnly() != null && request.getPublishedOnly()) {
                params.add("published");
            }
        }
        
        return params;
    }
}