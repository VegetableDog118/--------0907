package com.powertrading.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.notification.entity.NotificationTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 通知模板Mapper接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Mapper
public interface NotificationTemplateMapper extends BaseMapper<NotificationTemplate> {

    /**
     * 根据模板编码查询模板
     *
     * @param templateCode 模板编码
     * @return 通知模板
     */
    @Select("SELECT * FROM notification_templates WHERE template_code = #{templateCode} AND status = 1")
    NotificationTemplate selectByTemplateCode(@Param("templateCode") String templateCode);

    /**
     * 分页查询模板
     *
     * @param page         分页参数
     * @param templateName 模板名称
     * @param templateType 模板类型
     * @param status       状态
     * @return 模板列表
     */
    @Select("<script>" +
            "SELECT * FROM notification_templates WHERE 1=1" +
            "<if test=\"templateName != null and templateName != ''\">" +
            " AND template_name LIKE CONCAT('%', #{templateName}, '%')" +
            "</if>" +
            "<if test=\"templateType != null and templateType != ''\">" +
            " AND template_type = #{templateType}" +
            "</if>" +
            "<if test='status != null'>" +
            " AND status = #{status}" +
            "</if>" +
            " ORDER BY create_time DESC" +
            "</script>")
    IPage<NotificationTemplate> selectTemplates(Page<NotificationTemplate> page,
                                                @Param("templateName") String templateName,
                                                @Param("templateType") String templateType,
                                                @Param("status") Integer status);

    /**
     * 根据类型查询启用的模板
     *
     * @param templateType 模板类型
     * @return 模板列表
     */
    @Select("SELECT * FROM notification_templates WHERE template_type = #{templateType} AND status = 1 ORDER BY create_time DESC")
    List<NotificationTemplate> selectEnabledByType(@Param("templateType") String templateType);

    /**
     * 检查模板编码是否存在
     *
     * @param templateCode 模板编码
     * @param excludeId    排除的ID
     * @return 是否存在
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM notification_templates WHERE template_code = #{templateCode}" +
            "<if test=\"excludeId != null and excludeId != ''\">" +
            " AND id != #{excludeId}" +
            "</if>" +
            "</script>")
    Long checkTemplateCodeExists(@Param("templateCode") String templateCode,
                                  @Param("excludeId") String excludeId);
}