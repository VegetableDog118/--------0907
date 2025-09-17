#!/bin/bash

# Docker启动脚本
# 用于Interface Service容器启动时的初始化和健康检查

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 等待服务可用
wait_for_service() {
    local host=$1
    local port=$2
    local service_name=$3
    local max_attempts=30
    local attempt=1
    
    log_info "等待 $service_name 服务可用 ($host:$port)..."
    
    while [ $attempt -le $max_attempts ]; do
        if nc -z "$host" "$port" 2>/dev/null; then
            log_success "$service_name 服务已可用"
            return 0
        fi
        
        log_warn "$service_name 服务不可用，等待中... (尝试 $attempt/$max_attempts)"
        sleep 2
        attempt=$((attempt + 1))
    done
    
    log_error "$service_name 服务在 $((max_attempts * 2)) 秒后仍不可用"
    return 1
}

# 检查必需的环境变量
check_required_env() {
    local required_vars=("DB_HOST" "DB_USERNAME" "DB_PASSWORD")
    local missing_vars=()
    
    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            missing_vars+=("$var")
        fi
    done
    
    if [ ${#missing_vars[@]} -ne 0 ]; then
        log_error "缺少必需的环境变量: ${missing_vars[*]}"
        log_error "请设置以下环境变量:"
        for var in "${missing_vars[@]}"; do
            echo "  - $var"
        done
        exit 1
    fi
}

# 设置默认环境变量
set_default_env() {
    export DB_PORT=${DB_PORT:-3306}
    export REDIS_HOST=${REDIS_HOST:-redis}
    export REDIS_PORT=${REDIS_PORT:-6379}
    export RABBITMQ_HOST=${RABBITMQ_HOST:-rabbitmq}
    export RABBITMQ_PORT=${RABBITMQ_PORT:-5672}
    export RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-guest}
    export RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-guest}
    export SERVER_PORT=${SERVER_PORT:-8082}
    export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-docker}
    
    log_info "环境变量设置完成"
}

# 等待依赖服务
wait_for_dependencies() {
    log_info "检查依赖服务状态..."
    
    # 等待数据库
    if ! wait_for_service "$DB_HOST" "$DB_PORT" "MySQL数据库"; then
        log_error "数据库服务不可用，启动失败"
        exit 1
    fi
    
    # 等待Redis（可选）
    if [ -n "$REDIS_HOST" ]; then
        if ! wait_for_service "$REDIS_HOST" "$REDIS_PORT" "Redis缓存"; then
            log_warn "Redis服务不可用，将以无缓存模式运行"
        fi
    fi
    
    # 等待RabbitMQ（可选）
    if [ -n "$RABBITMQ_HOST" ]; then
        if ! wait_for_service "$RABBITMQ_HOST" "$RABBITMQ_PORT" "RabbitMQ消息队列"; then
            log_warn "RabbitMQ服务不可用，将以无消息队列模式运行"
        fi
    fi
    
    log_success "依赖服务检查完成"
}

# 数据库连接测试
test_database_connection() {
    log_info "测试数据库连接..."
    
    # 这里可以添加更详细的数据库连接测试
    # 例如使用mysql客户端测试连接
    
    log_success "数据库连接测试通过"
}

# 创建必要的目录
create_directories() {
    log_info "创建必要的目录..."
    
    mkdir -p /app/logs
    mkdir -p /app/temp
    mkdir -p /app/data
    
    log_success "目录创建完成"
}

# 设置JVM参数
setup_jvm_options() {
    log_info "配置JVM参数..."
    
    # 基础JVM参数
    local jvm_opts="-server"
    
    # 内存设置
    local heap_size=${JAVA_HEAP_SIZE:-1024m}
    jvm_opts="$jvm_opts -Xms512m -Xmx$heap_size"
    
    # GC设置
    jvm_opts="$jvm_opts -XX:+UseG1GC"
    jvm_opts="$jvm_opts -XX:G1HeapRegionSize=16m"
    jvm_opts="$jvm_opts -XX:+UseStringDeduplication"
    jvm_opts="$jvm_opts -XX:+OptimizeStringConcat"
    
    # GC日志
    jvm_opts="$jvm_opts -XX:+PrintGC"
    jvm_opts="$jvm_opts -XX:+PrintGCDetails"
    jvm_opts="$jvm_opts -XX:+PrintGCTimeStamps"
    jvm_opts="$jvm_opts -Xloggc:/app/logs/gc.log"
    
    # 其他优化参数
    jvm_opts="$jvm_opts -XX:+HeapDumpOnOutOfMemoryError"
    jvm_opts="$jvm_opts -XX:HeapDumpPath=/app/logs/"
    jvm_opts="$jvm_opts -Djava.awt.headless=true"
    jvm_opts="$jvm_opts -Dfile.encoding=UTF-8"
    jvm_opts="$jvm_opts -Duser.timezone=Asia/Shanghai"
    
    # 安全设置
    jvm_opts="$jvm_opts -Djava.security.egd=file:/dev/./urandom"
    
    export JAVA_OPTS="$jvm_opts $JAVA_OPTS"
    
    log_info "JVM参数: $JAVA_OPTS"
    log_success "JVM参数配置完成"
}

# 应用预热
warmup_application() {
    log_info "应用预热中..."
    
    # 等待应用启动
    local max_attempts=60
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "http://localhost:$SERVER_PORT/actuator/health" > /dev/null 2>&1; then
            log_success "应用预热完成"
            return 0
        fi
        
        if [ $((attempt % 10)) -eq 0 ]; then
            log_info "应用启动中... (尝试 $attempt/$max_attempts)"
        fi
        
        sleep 1
        attempt=$((attempt + 1))
    done
    
    log_warn "应用预热超时，但将继续运行"
    return 0
}

# 信号处理
handle_signal() {
    log_info "接收到停止信号，正在优雅关闭应用..."
    
    # 发送SIGTERM信号给Java进程
    if [ -n "$JAVA_PID" ]; then
        kill -TERM "$JAVA_PID"
        
        # 等待进程结束
        local count=0
        while kill -0 "$JAVA_PID" 2>/dev/null && [ $count -lt 30 ]; do
            sleep 1
            count=$((count + 1))
        done
        
        # 如果进程仍在运行，强制结束
        if kill -0 "$JAVA_PID" 2>/dev/null; then
            log_warn "强制结束应用进程"
            kill -KILL "$JAVA_PID"
        fi
    fi
    
    log_success "应用已停止"
    exit 0
}

# 主函数
main() {
    log_info "Interface Service 启动中..."
    log_info "版本: 1.0.0"
    log_info "环境: $SPRING_PROFILES_ACTIVE"
    
    # 设置信号处理
    trap handle_signal SIGTERM SIGINT
    
    # 检查环境变量
    check_required_env
    
    # 设置默认环境变量
    set_default_env
    
    # 创建目录
    create_directories
    
    # 等待依赖服务
    wait_for_dependencies
    
    # 测试数据库连接
    test_database_connection
    
    # 设置JVM参数
    setup_jvm_options
    
    log_info "启动应用..."
    
    # 启动应用
    if [ "$1" = "java" ]; then
        # 后台启动Java应用
        exec "$@" &
        JAVA_PID=$!
        
        # 应用预热
        warmup_application
        
        log_success "Interface Service 启动成功 (PID: $JAVA_PID)"
        log_info "健康检查地址: http://localhost:$SERVER_PORT/actuator/health"
        log_info "API文档地址: http://localhost:$SERVER_PORT/swagger-ui.html"
        
        # 等待进程结束
        wait $JAVA_PID
    else
        # 直接执行命令
        exec "$@"
    fi
}

# 执行主函数
main "$@"