#!/bin/bash

# 接口平台后端服务启动脚本
# 一次性启动所有微服务

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
JAVA_HOME="/Library/Java/JavaVirtualMachines/graalvm-ce-java17-22.3.0/Contents/Home"
BACKEND_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$BACKEND_DIR/logs"
PID_DIR="$BACKEND_DIR/pids"

# 创建必要的目录
mkdir -p "$LOG_DIR" "$PID_DIR"

# 服务配置 (服务名:端口)
SERVICES=(
    "gateway-service:8090"
    "auth-service:8081"
    "interface-service:8083"
    "notification-service:8084"
    "approval-service:8085"
    "user-service:8087"
    "datasource-service:8088"
)

# 获取服务端口
get_service_port() {
    local service=$1
    for item in "${SERVICES[@]}"; do
        if [[ "$item" == "$service:"* ]]; then
            echo "${item#*:}"
            return
        fi
    done
    echo ""
}

# 获取所有服务名
get_all_services() {
    for item in "${SERVICES[@]}"; do
        echo "${item%:*}"
    done
}

# 打印带颜色的消息
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# 检查端口是否被占用
check_port() {
    local port=$1
    lsof -ti:$port > /dev/null 2>&1
}

# 等待服务启动
wait_for_service() {
    local service=$1
    local port=$2
    local max_wait=60
    local count=0
    
    print_message $YELLOW "等待 $service 启动 (端口 $port)..."
    
    while [ $count -lt $max_wait ]; do
        if check_port $port; then
            print_message $GREEN "✓ $service 启动成功 (端口 $port)"
            return 0
        fi
        sleep 2
        count=$((count + 2))
        echo -n "."
    done
    
    echo
    print_message $RED "✗ $service 启动超时 (端口 $port)"
    return 1
}

# 启动单个服务
start_service() {
    local service=$1
    local port=$2
    local service_dir="$BACKEND_DIR/$service"
    local log_file="$LOG_DIR/$service.log"
    local pid_file="$PID_DIR/$service.pid"
    
    # 检查服务目录是否存在
    if [ ! -d "$service_dir" ]; then
        print_message $RED "错误: 服务目录不存在: $service_dir"
        return 1
    fi
    
    # 检查端口是否已被占用
    if check_port $port; then
        print_message $YELLOW "警告: 端口 $port 已被占用，跳过 $service"
        return 0
    fi
    
    print_message $BLUE "启动 $service..."
    
    # 切换到服务目录并启动
    cd "$service_dir"
    
    # 启动服务
    export JAVA_HOME="$JAVA_HOME"
    nohup mvn spring-boot:run -Dmaven.test.skip=true > "$log_file" 2>&1 &
    local pid=$!
    
    # 保存PID
    echo $pid > "$pid_file"
    
    # 等待服务启动
    if wait_for_service $service $port; then
        print_message $GREEN "$service 启动完成 (PID: $pid)"
        return 0
    else
        print_message $RED "$service 启动失败"
        # 清理失败的进程
        if kill -0 $pid 2>/dev/null; then
            kill $pid
        fi
        rm -f "$pid_file"
        return 1
    fi
}

# 检查Java环境
check_java() {
    if [ ! -d "$JAVA_HOME" ]; then
        print_message $RED "错误: JAVA_HOME 路径不存在: $JAVA_HOME"
        exit 1
    fi
    
    export JAVA_HOME="$JAVA_HOME"
    if ! command -v java &> /dev/null; then
        print_message $RED "错误: Java 命令不可用"
        exit 1
    fi
    
    print_message $GREEN "Java 环境检查通过: $(java -version 2>&1 | head -n 1)"
}

# 检查Maven环境
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_message $RED "错误: Maven 命令不可用"
        exit 1
    fi
    
    print_message $GREEN "Maven 环境检查通过: $(mvn -version | head -n 1)"
}

# 显示服务状态
show_status() {
    print_message $BLUE "\n=== 服务状态 ==="
    
    for item in "${SERVICES[@]}"; do
        local service="${item%:*}"
        local port="${item#*:}"
        local pid_file="$PID_DIR/$service.pid"
        
        if [ -f "$pid_file" ]; then
            local pid=$(cat "$pid_file")
            if kill -0 $pid 2>/dev/null && check_port $port; then
                print_message $GREEN "✓ $service (PID: $pid, 端口: $port) - 运行中"
            else
                print_message $RED "✗ $service - 已停止"
                rm -f "$pid_file"
            fi
        else
            print_message $RED "✗ $service - 未启动"
        fi
    done
}

# 主函数
main() {
    print_message $BLUE "=== 接口平台后端服务启动脚本 ==="
    print_message $BLUE "启动时间: $(date)"
    
    # 环境检查
    check_java
    check_maven
    
    # 编译项目
    print_message $BLUE "\n编译整个项目..."
    cd "$BACKEND_DIR"
    if mvn clean compile -Dmaven.test.skip=true; then
        print_message $GREEN "项目编译成功"
    else
        print_message $RED "项目编译失败，退出"
        exit 1
    fi
    
    # 启动服务
    print_message $BLUE "\n开始启动服务..."
    local success_count=0
    local total_count=${#SERVICES[@]}
    
    for item in "${SERVICES[@]}"; do
        local service="${item%:*}"
        local port="${item#*:}"
        if start_service $service $port; then
            success_count=$((success_count + 1))
        fi
        echo
    done
    
    # 显示最终状态
    show_status
    
    print_message $BLUE "\n=== 启动完成 ==="
    print_message $GREEN "成功启动: $success_count/$total_count 个服务"
    
    if [ $success_count -eq $total_count ]; then
        print_message $GREEN "所有服务启动成功！"
        print_message $BLUE "网关地址: http://localhost:8090"
        print_message $BLUE "日志目录: $LOG_DIR"
        print_message $BLUE "使用 './stop-all-services.sh' 停止所有服务"
    else
        print_message $YELLOW "部分服务启动失败，请检查日志"
    fi
}

# 信号处理
trap 'print_message $RED "\n脚本被中断"; exit 1' INT TERM

# 执行主函数
main "$@"