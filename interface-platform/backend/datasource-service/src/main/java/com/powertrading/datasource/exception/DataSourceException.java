package com.powertrading.datasource.exception;

/**
 * 数据源异常类
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
public class DataSourceException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 数据源ID
     */
    private Long dataSourceId;

    public DataSourceException() {
        super();
    }

    public DataSourceException(String message) {
        super(message);
    }

    public DataSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataSourceException(Throwable cause) {
        super(cause);
    }

    public DataSourceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DataSourceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public DataSourceException(String errorCode, String message, Long dataSourceId) {
        super(message);
        this.errorCode = errorCode;
        this.dataSourceId = dataSourceId;
    }

    public DataSourceException(String errorCode, String message, Long dataSourceId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.dataSourceId = dataSourceId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Long getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(Long dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        
        if (errorCode != null) {
            sb.append("[errorCode=").append(errorCode).append("]");
        }
        
        if (dataSourceId != null) {
            sb.append("[dataSourceId=").append(dataSourceId).append("]");
        }
        
        String message = getMessage();
        if (message != null) {
            sb.append(": ").append(message);
        }
        
        return sb.toString();
    }
}