#!/bin/bash

# 接口平台后端服务停止脚本
# 停止所有运行的微服务

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
BACKEND_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$BACKEND_DIR/logs"
PID_DIR="$BACKEND_DIR/pids"

# 服务配置 (服务名:端口) - 停止顺序与启动相反
SERVICES=(
    "datasource-service:8088"
    "user-service:8087"
    "approval-service:8085"
    "notification-service:8084"
    "interface-service:8083"
    "auth-service:8081"
    "gateway-service:8080"
)

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

# 停止单个服务
stop_service() {
    local service=$1
    local port=$2
    local pid_file="$PID_DIR/$service.pid"
    local stopped=false
    
    print_message $BLUE "停止 $service..."
    
    # 通过PID文件停止
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if kill -0 $pid 2>/dev/null; then
            print_message $YELLOW "发送TERM信号到进程 $pid"
            kill $pid
            
            # 等待进程优雅退出
            local count=0
            while [ $count -lt 15 ]; do
                if ! kill -0 $pid 2>/dev/null; then
                    print_message $GREEN "✓ $service 已优雅停止"
                    stopped=true
                    break
                fi
                sleep 1
                count=$((count + 1))
                echo -n "."
            done
            
            # 如果优雅停止失败，强制杀死
            if ! $stopped && kill -0 $pid 2>/dev/null; then
                print_message $YELLOW "\n强制停止进程 $pid"
                kill -9 $pid
                sleep 1
                if ! kill -0 $pid 2>/dev/null; then
                    print_message $GREEN "✓ $service 已强制停止"
                    stopped=true
                fi
            fi
        else
            print_message $YELLOW "PID文件存在但进程不存在: $pid"
        fi
        
        # 清理PID文件
        rm -f "$pid_file"
    fi
    
    # 通过端口查找并停止进程
    if ! $stopped && check_port $port; then
        print_message $YELLOW "通过端口 $port 查找进程"
        local pids=$(lsof -ti:$port)
        if [ -n "$pids" ]; then
            for pid in $pids; do
                print_message $YELLOW "停止占用端口 $port 的进程 $pid"
                kill $pid 2>/dev/null
                sleep 2
                if kill -0 $pid 2>/dev/null; then
                    kill -9 $pid 2>/dev/null
                fi
            done
            
            # 再次检查端口
            if ! check_port $port; then
                print_message $GREEN "✓ $service 端口 $port 已释放"
                stopped=true
            fi
        fi
    fi
    
    if $stopped || ! check_port $port; then
        print_message $GREEN "✓ $service 停止完成"
        return 0
    else
        print_message $RED "✗ $service 停止失败"
        return 1
    fi
}

# 停止所有Maven进程
stop_maven_processes() {
    print_message $BLUE "查找并停止Maven进程..."
    
    # 查找spring-boot:run进程
    local maven_pids=$(pgrep -f "spring-boot:run")
    if [ -n "$maven_pids" ]; then
        print_message $YELLOW "发现Maven进程: $maven_pids"
        for pid in $maven_pids; do
            print_message $YELLOW "停止Maven进程 $pid"
            kill $pid 2>/dev/null
            sleep 2
            if kill -0 $pid 2>/dev/null; then
                kill -9 $pid 2>/dev/null
            fi
        done
    else
        print_message $GREEN "未发现运行中的Maven进程"
    fi
}

# 清理资源
cleanup_resources() {
    print_message $BLUE "清理资源..."
    
    # 清理所有PID文件
    if [ -d "$PID_DIR" ]; then
        rm -f "$PID_DIR"/*.pid
        print_message $GREEN "✓ 清理PID文件"
    fi
    
    # 检查并清理孤立进程
    for service in "${!SERVICES[@]}"; do
        local port=${SERVICES[$service]}
        if check_port $port; then
            print_message $YELLOW "发现孤立进程占用端口 $port"
            local pids=$(lsof -ti:$port)
            for pid in $pids; do
                kill -9 $pid 2>/dev/null
            done
        fi
    done
}

# 显示服务状态
show_status() {
    print_message $BLUE "\n=== 服务状态 ==="
    
    local running_count=0
    for item in "${SERVICES[@]}"; do
        local service="${item%:*}"
        local port="${item#*:}"
        if check_port $port; then
            local pids=$(lsof -ti:$port)
            print_message $RED "✗ $service (端口: $port, PID: $pids) - 仍在运行"
            running_count=$((running_count + 1))
        else
            print_message $GREEN "✓ $service (端口: $port) - 已停止"
        fi
    done
    
    return $running_count
}

# 强制停止所有相关进程
force_stop_all() {
    print_message $YELLOW "\n执行强制停止..."
    
    # 停止所有spring-boot进程
    pkill -f "spring-boot:run" 2>/dev/null
    
    # 停止所有Java进程（谨慎使用）
    # pkill -f "java.*spring-boot" 2>/dev/null
    
    # 释放所有相关端口
    for item in "${SERVICES[@]}"; do
        local port="${item#*:}"
        if check_port $port; then
            local pids=$(lsof -ti:$port)
            for pid in $pids; do
                kill -9 $pid 2>/dev/null
            done
        fi
    done
    
    sleep 2
}

# 主函数
main() {
    local force_mode=false
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -f|--force)
                force_mode=true
                shift
                ;;
            -h|--help)
                print_message $BLUE "用法: $0 [-f|--force] [-h|--help]"
                print_message $BLUE "  -f, --force    强制停止所有相关进程"
                print_message $BLUE "  -h, --help     显示帮助信息"
                exit 0
                ;;
            *)
                print_message $RED "未知参数: $1"
                exit 1
                ;;
        esac
    done
    
    print_message $BLUE "=== 接口平台后端服务停止脚本 ==="
    print_message $BLUE "停止时间: $(date)"
    
    if $force_mode; then
        print_message $YELLOW "强制模式已启用"
    fi
    
    # 创建必要的目录
    mkdir -p "$PID_DIR"
    
    # 停止服务
    print_message $BLUE "\n开始停止服务..."
    local success_count=0
    local total_count=${#SERVICES[@]}
    
    for item in "${SERVICES[@]}"; do
        local service="${item%:*}"
        local port="${item#*:}"
        if stop_service $service $port; then
            success_count=$((success_count + 1))
        fi
        echo
    done
    
    # 停止Maven进程
    stop_maven_processes
    
    # 显示状态
    show_status
    local remaining_count=$?
    
    # 如果还有进程运行，询问是否强制停止
    if [ $remaining_count -gt 0 ]; then
        if $force_mode; then
            force_stop_all
        else
            print_message $YELLOW "\n仍有 $remaining_count 个服务在运行"
            read -p "是否强制停止所有相关进程? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                force_stop_all
            fi
        fi
        
        # 再次检查状态
        show_status
        remaining_count=$?
    fi
    
    # 清理资源
    cleanup_resources
    
    # 最终报告
    print_message $BLUE "\n=== 停止完成 ==="
    if [ $remaining_count -eq 0 ]; then
        print_message $GREEN "所有服务已成功停止！"
    else
        print_message $YELLOW "仍有 $remaining_count 个服务在运行，可能需要手动处理"
    fi
}

# 信号处理
trap 'print_message $RED "\n脚本被中断"; exit 1' INT TERM

# 执行主函数
main "$@"