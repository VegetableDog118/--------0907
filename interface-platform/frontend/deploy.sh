#!/bin/bash

# 接口平台前端自动部署脚本
# 使用方法：./deploy.sh [production|staging]

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查参数
ENV=${1:-production}
if [[ "$ENV" != "production" && "$ENV" != "staging" ]]; then
    log_error "无效的环境参数: $ENV"
    log_info "使用方法: $0 [production|staging]"
    exit 1
fi

log_info "开始部署到 $ENV 环境..."

# 检查必要的工具
check_dependencies() {
    log_info "检查依赖工具..."
    
    if ! command -v node &> /dev/null; then
        log_error "Node.js 未安装"
        exit 1
    fi
    
    if ! command -v npm &> /dev/null; then
        log_error "npm 未安装"
        exit 1
    fi
    
    log_success "依赖检查完成"
}

# 安装依赖
install_dependencies() {
    log_info "安装项目依赖..."
    npm ci
    log_success "依赖安装完成"
}

# 构建项目
build_project() {
    log_info "构建项目..."
    
    # 设置环境变量
    if [[ "$ENV" == "production" ]]; then
        export NODE_ENV=production
        log_info "使用生产环境配置"
    else
        export NODE_ENV=staging
        log_info "使用测试环境配置"
    fi
    
    # 执行构建
    npm run build
    
    # 检查构建结果
    if [[ ! -d "dist" ]]; then
        log_error "构建失败：dist目录不存在"
        exit 1
    fi
    
    log_success "项目构建完成"
}

# 创建部署包
create_deployment_package() {
    log_info "创建部署包..."
    
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    PACKAGE_NAME="interface-platform-frontend-${ENV}-${TIMESTAMP}.tar.gz"
    
    # 创建临时目录
    TEMP_DIR="temp_deploy_${TIMESTAMP}"
    mkdir -p "$TEMP_DIR"
    
    # 复制必要文件
    cp -r dist "$TEMP_DIR/"
    cp nginx.conf "$TEMP_DIR/"
    cp Dockerfile "$TEMP_DIR/"
    cp docker-compose.yml "$TEMP_DIR/"
    cp DEPLOYMENT.md "$TEMP_DIR/"
    
    # 创建部署信息文件
    cat > "$TEMP_DIR/deploy-info.txt" << EOF
部署信息
========
环境: $ENV
构建时间: $(date)
构建版本: $(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
构建分支: $(git branch --show-current 2>/dev/null || echo "unknown")
Node版本: $(node --version)
npm版本: $(npm --version)
EOF
    
    # 打包
    tar -czf "$PACKAGE_NAME" -C "$TEMP_DIR" .
    
    # 清理临时目录
    rm -rf "$TEMP_DIR"
    
    log_success "部署包创建完成: $PACKAGE_NAME"
    echo "$PACKAGE_NAME"
}

# Docker部署
deploy_with_docker() {
    log_info "使用Docker部署..."
    
    # 构建镜像
    DOCKER_TAG="interface-platform-frontend:${ENV}-$(date +"%Y%m%d_%H%M%S")"
    docker build -t "$DOCKER_TAG" .
    
    # 停止旧容器
    if docker ps -q -f name=interface-platform-frontend-$ENV; then
        log_info "停止旧容器..."
        docker stop interface-platform-frontend-$ENV
        docker rm interface-platform-frontend-$ENV
    fi
    
    # 启动新容器
    docker run -d \
        --name interface-platform-frontend-$ENV \
        -p 80:80 \
        --restart unless-stopped \
        "$DOCKER_TAG"
    
    log_success "Docker部署完成"
}

# 健康检查
health_check() {
    log_info "执行健康检查..."
    
    # 等待服务启动
    sleep 5
    
    # 检查HTTP响应
    if curl -f -s http://localhost/ > /dev/null; then
        log_success "健康检查通过"
    else
        log_error "健康检查失败"
        exit 1
    fi
}

# 主函数
main() {
    log_info "=== 接口平台前端部署脚本 ==="
    log_info "环境: $ENV"
    log_info "时间: $(date)"
    echo
    
    # 执行部署步骤
    check_dependencies
    install_dependencies
    build_project
    
    # 创建部署包
    PACKAGE_FILE=$(create_deployment_package)
    
    # 询问部署方式
    echo
    log_info "选择部署方式:"
    echo "1) 仅创建部署包（手动部署）"
    echo "2) Docker自动部署"
    read -p "请选择 (1-2): " DEPLOY_CHOICE
    
    case $DEPLOY_CHOICE in
        1)
            log_info "部署包已创建: $PACKAGE_FILE"
            log_info "请参考 DEPLOYMENT.md 进行手动部署"
            ;;
        2)
            if command -v docker &> /dev/null; then
                deploy_with_docker
                health_check
            else
                log_error "Docker 未安装，无法自动部署"
                exit 1
            fi
            ;;
        *)
            log_error "无效选择"
            exit 1
            ;;
    esac
    
    echo
    log_success "=== 部署完成 ==="
    log_info "部署包: $PACKAGE_FILE"
    log_info "环境: $ENV"
    log_info "时间: $(date)"
    
    if [[ $DEPLOY_CHOICE == "2" ]]; then
        log_info "访问地址: http://localhost"
        log_info "容器名称: interface-platform-frontend-$ENV"
    fi
}

# 捕获中断信号
trap 'log_error "部署被中断"; exit 1' INT TERM

# 执行主函数
main "$@"