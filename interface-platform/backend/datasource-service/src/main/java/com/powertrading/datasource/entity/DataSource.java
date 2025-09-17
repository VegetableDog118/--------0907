package com.powertrading.datasource.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 数据源实体类
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@Entity
@Table(name = "datasources", indexes = {
    @Index(name = "idx_datasource_name", columnList = "name"),
    @Index(name = "idx_datasource_type", columnList = "type"),
    @Index(name = "idx_datasource_status", columnList = "status")
})
public class DataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "数据源名称不能为空")
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @NotBlank(message = "数据源类型不能为空")
    @Column(name = "type", nullable = false, length = 50)
    private String type; // mysql, postgresql, oracle, sqlserver

    @NotBlank(message = "数据库URL不能为空")
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @NotBlank(message = "用户名不能为空")
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @JsonIgnore
    @NotBlank(message = "密码不能为空")
    @Column(name = "password", nullable = false, length = 200)
    private String password;

    @Column(name = "driver_class", length = 200)
    private String driverClass;

    @NotNull(message = "状态不能为空")
    @Column(name = "status", nullable = false)
    private Integer status; // 0-禁用, 1-启用, 2-异常

    // 连接池配置
    @Column(name = "min_pool_size")
    private Integer minPoolSize = 2;

    @Column(name = "max_pool_size")
    private Integer maxPoolSize = 10;

    @Column(name = "connection_timeout")
    private Long connectionTimeout = 30000L;

    @Column(name = "idle_timeout")
    private Long idleTimeout = 600000L;

    @Column(name = "max_lifetime")
    private Long maxLifetime = 1800000L;

    @Column(name = "validation_timeout")
    private Long validationTimeout = 5000L;

    @Column(name = "leak_detection_threshold")
    private Long leakDetectionThreshold = 60000L;

    // 扩展配置（JSON格式）
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    // 创建者和更新者
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 最后连接时间
    @Column(name = "last_connected_at")
    private LocalDateTime lastConnectedAt;

    // 最后健康检查时间
    @Column(name = "last_health_check_at")
    private LocalDateTime lastHealthCheckAt;

    // 健康检查状态
    @Column(name = "health_status")
    private Integer healthStatus; // 0-未知, 1-健康, 2-异常

    // 错误信息
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    // 构造函数
    public DataSource() {}

    public DataSource(String name, String type, String url, String username, String password) {
        this.name = name;
        this.type = type;
        this.url = url;
        this.username = username;
        this.password = password;
        this.status = 1; // 默认启用
        this.healthStatus = 0; // 默认未知
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(Integer minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public Long getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(Long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public Long getValidationTimeout() {
        return validationTimeout;
    }

    public void setValidationTimeout(Long validationTimeout) {
        this.validationTimeout = validationTimeout;
    }

    public Long getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }

    public void setLeakDetectionThreshold(Long leakDetectionThreshold) {
        this.leakDetectionThreshold = leakDetectionThreshold;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastConnectedAt() {
        return lastConnectedAt;
    }

    public void setLastConnectedAt(LocalDateTime lastConnectedAt) {
        this.lastConnectedAt = lastConnectedAt;
    }

    public LocalDateTime getLastHealthCheckAt() {
        return lastHealthCheckAt;
    }

    public void setLastHealthCheckAt(LocalDateTime lastHealthCheckAt) {
        this.lastHealthCheckAt = lastHealthCheckAt;
    }

    public Integer getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(Integer healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSource that = (DataSource) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "DataSource{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", status=" + status +
                ", healthStatus=" + healthStatus +
                '}';
    }
}