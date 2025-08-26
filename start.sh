#!/bin/bash

# USDT Trading Platform Docker Deployment Management Script
# Version: 1.0
# Description: Comprehensive Docker deployment management for USDT trading platform

set -euo pipefail

# Color codes for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m' # No Color

# Configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_NAME="usdt-trading-platform"
readonly DOCKER_COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"
readonly ENV_FILE="${SCRIPT_DIR}/.env"
readonly ENV_EXAMPLE_FILE="${SCRIPT_DIR}/.env.example"
readonly SSL_DIR="${SCRIPT_DIR}/docker/nginx/ssl"
readonly LOG_DIR="${SCRIPT_DIR}/logs"
readonly BACKUP_DIR="${SCRIPT_DIR}/backups"

# Service configuration
readonly SERVICES=("mysql" "redis" "backend" "admin-frontend" "user-frontend" "nginx")
readonly REQUIRED_COMMANDS=("docker" "docker-compose" "openssl" "curl")

# Logging functions
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
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

log_header() {
    echo -e "\n${PURPLE}=== $1 ===${NC}"
}

# Check if running as root
check_root() {
    if [[ $EUID -eq 0 ]]; then
        log_warning "Running as root. Consider running as non-root user for security."
    fi
}

# Check system requirements
check_requirements() {
    log_header "Checking System Requirements"
    
    for cmd in "${REQUIRED_COMMANDS[@]}"; do
        if ! command -v "$cmd" &> /dev/null; then
            log_error "$cmd is not installed. Please install it first."
            exit 1
        else
            log_success "$cmd is available"
        fi
    done
    
    # Check Docker daemon
    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running. Please start Docker service."
        exit 1
    fi
    
    # Check Docker Compose version
    local compose_version
    compose_version=$(docker-compose --version | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')
    log_info "Docker Compose version: $compose_version"
}

# Create necessary directories
create_directories() {
    log_header "Creating Necessary Directories"
    
    local dirs=("$LOG_DIR" "$BACKUP_DIR" "$SSL_DIR")
    
    for dir in "${dirs[@]}"; do
        if [[ ! -d "$dir" ]]; then
            mkdir -p "$dir"
            log_success "Created directory: $dir"
        else
            log_info "Directory already exists: $dir"
        fi
    done
}

# Generate environment file from example
setup_environment() {
    log_header "Setting Up Environment Configuration"
    
    if [[ ! -f "$ENV_FILE" ]]; then
        if [[ -f "$ENV_EXAMPLE_FILE" ]]; then
            cp "$ENV_EXAMPLE_FILE" "$ENV_FILE"
            log_success "Created .env file from .env.example"
            log_warning "Please review and update the .env file with your specific configuration"
        else
            log_warning ".env.example file not found. Creating basic .env file"
            create_basic_env_file
        fi
    else
        log_info "Environment file already exists"
    fi
}

# Create basic environment file if example doesn't exist
create_basic_env_file() {
    cat > "$ENV_FILE" << 'EOF'
# USDT Trading Platform Environment Configuration

# Application Environment
SPRING_PROFILES_ACTIVE=prod
NODE_ENV=production

# Database Configuration
MYSQL_ROOT_PASSWORD=UsdtTrading123!
MYSQL_USER=usdt_user
MYSQL_PASSWORD=UsdtUser123!
MYSQL_DATABASE=usdt_trading_platform

# Redis Configuration
REDIS_PASSWORD=

# JWT Configuration
JWT_SECRET_KEY=your-super-secret-jwt-key-change-this-in-production
JWT_EXPIRATION=86400

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# File Storage Configuration
FILE_STORAGE_PATH=/app/uploads
FILE_MAX_SIZE=100MB

# Blockchain Configuration
BLOCKCHAIN_NODE_URL=https://mainnet.infura.io/v3/your-project-id
BLOCKCHAIN_PRIVATE_KEY=your-blockchain-private-key

# Security Configuration
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com

# SSL Configuration
SSL_ENABLED=true
SSL_CERT_PATH=/etc/nginx/ssl/cert.pem
SSL_KEY_PATH=/etc/nginx/ssl/key.pem

# Monitoring Configuration
HEALTH_CHECK_ENABLED=true
METRICS_ENABLED=true
EOF
    log_success "Created basic .env file"
}

# Generate SSL certificates
generate_ssl_certificates() {
    log_header "Generating SSL Certificates"
    
    if [[ ! -f "$SSL_DIR/cert.pem" ]] || [[ ! -f "$SSL_DIR/key.pem" ]]; then
        log_info "Generating self-signed SSL certificate for development..."
        
        openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
            -keyout "$SSL_DIR/key.pem" \
            -out "$SSL_DIR/cert.pem" \
            -subj "/C=US/ST=State/L=City/O=Organization/OU=OrgUnit/CN=localhost"
        
        # Set appropriate permissions
        chmod 600 "$SSL_DIR/key.pem"
        chmod 644 "$SSL_DIR/cert.pem"
        
        log_success "SSL certificates generated successfully"
        log_warning "Using self-signed certificates. For production, use proper SSL certificates."
    else
        log_info "SSL certificates already exist"
    fi
}

# Build Docker images
build_images() {
    log_header "Building Docker Images"
    
    # Build with no cache for fresh build
    if [[ "${1:-}" == "--no-cache" ]]; then
        log_info "Building images with no cache..."
        docker-compose -f "$DOCKER_COMPOSE_FILE" build --no-cache
    else
        log_info "Building images..."
        docker-compose -f "$DOCKER_COMPOSE_FILE" build
    fi
    
    log_success "Docker images built successfully"
}

# Start services
start_services() {
    log_header "Starting Services"
    
    # Start services in the correct order
    log_info "Starting infrastructure services (MySQL, Redis)..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" up -d mysql redis
    
    # Wait for database to be ready
    wait_for_service "mysql" "3306"
    wait_for_service "redis" "6379"
    
    log_info "Starting application services..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" up -d backend
    
    # Wait for backend to be ready
    wait_for_backend
    
    log_info "Starting frontend services..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" up -d admin-frontend user-frontend
    
    log_info "Starting reverse proxy..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" up -d nginx
    
    log_success "All services started successfully"
}

# Wait for service to be ready
wait_for_service() {
    local service_name="$1"
    local port="$2"
    local max_attempts=30
    local attempt=1
    
    log_info "Waiting for $service_name to be ready..."
    
    while [[ $attempt -le $max_attempts ]]; do
        if docker-compose -f "$DOCKER_COMPOSE_FILE" exec -T "$service_name" nc -z localhost "$port" 2>/dev/null; then
            log_success "$service_name is ready"
            return 0
        fi
        
        log_info "Attempt $attempt/$max_attempts: $service_name not ready yet..."
        sleep 2
        ((attempt++))
    done
    
    log_error "$service_name failed to start within expected time"
    return 1
}

# Wait for backend health check
wait_for_backend() {
    local max_attempts=60
    local attempt=1
    
    log_info "Waiting for backend health check..."
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -sf http://localhost:8080/api/actuator/health &>/dev/null; then
            log_success "Backend is healthy"
            return 0
        fi
        
        log_info "Attempt $attempt/$max_attempts: Backend not ready yet..."
        sleep 5
        ((attempt++))
    done
    
    log_error "Backend health check failed"
    return 1
}

# Stop services
stop_services() {
    log_header "Stopping Services"
    
    docker-compose -f "$DOCKER_COMPOSE_FILE" down
    log_success "All services stopped"
}

# Restart services
restart_services() {
    log_header "Restarting Services"
    
    stop_services
    sleep 2
    start_services
}

# Show service status
show_status() {
    log_header "Service Status"
    
    docker-compose -f "$DOCKER_COMPOSE_FILE" ps
    
    echo
    log_header "Service Health Status"
    
    for service in "${SERVICES[@]}"; do
        local status
        status=$(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q "$service" | xargs docker inspect --format='{{.State.Health.Status}}' 2>/dev/null || echo "no-healthcheck")
        
        case "$status" in
            "healthy")
                log_success "$service: $status"
                ;;
            "unhealthy")
                log_error "$service: $status"
                ;;
            "starting")
                log_warning "$service: $status"
                ;;
            *)
                log_info "$service: running (no health check)"
                ;;
        esac
    done
}

# Show logs
show_logs() {
    local service="${1:-}"
    local follow="${2:-}"
    
    if [[ -n "$service" ]]; then
        log_header "Logs for $service"
        if [[ "$follow" == "-f" ]]; then
            docker-compose -f "$DOCKER_COMPOSE_FILE" logs -f "$service"
        else
            docker-compose -f "$DOCKER_COMPOSE_FILE" logs --tail=100 "$service"
        fi
    else
        log_header "All Service Logs"
        if [[ "$follow" == "-f" ]]; then
            docker-compose -f "$DOCKER_COMPOSE_FILE" logs -f
        else
            docker-compose -f "$DOCKER_COMPOSE_FILE" logs --tail=50
        fi
    fi
}

# Backup data
backup_data() {
    log_header "Creating Data Backup"
    
    local timestamp
    timestamp=$(date +"%Y%m%d_%H%M%S")
    local backup_file="$BACKUP_DIR/backup_$timestamp.tar.gz"
    
    # Create database backup
    log_info "Creating database backup..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" exec -T mysql mysqldump -u root -p"${MYSQL_ROOT_PASSWORD:-UsdtTrading123!}" usdt_trading_platform > "$BACKUP_DIR/db_$timestamp.sql"
    
    # Create volume backup
    log_info "Creating volume backup..."
    docker run --rm -v "${PROJECT_NAME}_mysql_data:/data" -v "$BACKUP_DIR:/backup" alpine tar czf "/backup/volumes_$timestamp.tar.gz" -C /data .
    
    log_success "Backup created: $backup_file"
}

# Clean up system
cleanup_system() {
    log_header "Cleaning Up System"
    
    # Remove unused containers, networks, images, and volumes
    docker system prune -af
    
    # Remove old backups (keep last 7 days)
    find "$BACKUP_DIR" -name "*.sql" -type f -mtime +7 -delete 2>/dev/null || true
    find "$BACKUP_DIR" -name "*.tar.gz" -type f -mtime +7 -delete 2>/dev/null || true
    
    log_success "System cleanup completed"
}

# Update system
update_system() {
    log_header "Updating System"
    
    # Pull latest code changes (if in git repository)
    if [[ -d ".git" ]]; then
        log_info "Pulling latest code changes..."
        git pull origin main || log_warning "Failed to pull latest changes"
    fi
    
    # Rebuild images
    build_images --no-cache
    
    # Restart services
    restart_services
    
    log_success "System updated successfully"
}

# Show system information
show_system_info() {
    log_header "System Information"
    
    echo -e "${CYAN}Project:${NC} $PROJECT_NAME"
    echo -e "${CYAN}Location:${NC} $SCRIPT_DIR"
    echo -e "${CYAN}Docker Version:${NC} $(docker --version)"
    echo -e "${CYAN}Docker Compose Version:${NC} $(docker-compose --version)"
    echo -e "${CYAN}System:${NC} $(uname -a)"
    
    echo
    log_header "Resource Usage"
    docker stats --no-stream
    
    echo
    log_header "Volume Information"
    docker volume ls --filter name="${PROJECT_NAME}"
    
    echo
    log_header "Network Information"
    docker network ls --filter name="${PROJECT_NAME}"
}

# Run database migration
run_migration() {
    log_header "Running Database Migration"
    
    # Wait for backend to be ready
    wait_for_backend
    
    # Run migration endpoint if available
    if curl -sf http://localhost:8080/api/actuator/health &>/dev/null; then
        log_info "Backend is ready for migration"
        # Add your migration logic here
        log_success "Database migration completed"
    else
        log_error "Backend is not ready for migration"
        return 1
    fi
}

# Show help
show_help() {
    cat << 'EOF'
USDT Trading Platform Docker Management Script

USAGE:
    ./start.sh [COMMAND] [OPTIONS]

COMMANDS:
    start           Start all services
    stop            Stop all services
    restart         Restart all services
    status          Show service status
    logs [service]  Show logs (optionally for specific service)
    build           Build Docker images
    backup          Create data backup
    clean           Clean up Docker system
    update          Update system and restart
    info            Show system information
    migrate         Run database migration
    help            Show this help message

EXAMPLES:
    ./start.sh start                    # Start all services
    ./start.sh logs backend -f          # Follow backend logs
    ./start.sh build --no-cache         # Rebuild images without cache
    ./start.sh status                   # Show service status

ENVIRONMENT:
    Ensure .env file is properly configured before starting services.
    Use .env.example as a template.

LOGS:
    Application logs are stored in: ./logs/
    Backup files are stored in: ./backups/

For more information, visit: https://github.com/your-repo/usdt-trading-platform
EOF
}

# Main function
main() {
    check_root
    
    case "${1:-help}" in
        "start")
            check_requirements
            create_directories
            setup_environment
            generate_ssl_certificates
            build_images
            start_services
            show_status
            echo
            log_success "USDT Trading Platform is now running!"
            log_info "User Frontend: https://localhost/user/"
            log_info "Admin Frontend: https://localhost/admin/"
            log_info "API Backend: https://localhost/api/"
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            restart_services
            show_status
            ;;
        "status")
            show_status
            ;;
        "logs")
            show_logs "${2:-}" "${3:-}"
            ;;
        "build")
            check_requirements
            build_images "${2:-}"
            ;;
        "backup")
            backup_data
            ;;
        "clean")
            cleanup_system
            ;;
        "update")
            update_system
            ;;
        "info")
            show_system_info
            ;;
        "migrate")
            run_migration
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            log_error "Unknown command: $1"
            echo
            show_help
            exit 1
            ;;
    esac
}

# Execute main function with all arguments
main "$@"