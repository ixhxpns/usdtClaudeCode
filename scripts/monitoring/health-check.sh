#!/bin/bash

# Health Check and Monitoring Script for USDT Trading Platform
# This script performs comprehensive health checks on all services

set -euo pipefail

# Color codes
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly NC='\033[0m'

# Configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly DOCKER_COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"
readonly LOG_FILE="${PROJECT_ROOT}/logs/health-check.log"

# Service configuration
readonly SERVICES=("mysql" "redis" "backend" "admin-frontend" "user-frontend" "nginx")
readonly HEALTH_ENDPOINTS=(
    "mysql:mysql://root:password@localhost:3306"
    "redis:redis://localhost:6379"
    "backend:http://localhost:8080/api/actuator/health"
    "admin-frontend:http://localhost:3000/"
    "user-frontend:http://localhost:3001/"
    "nginx:http://localhost:80/health"
)

# Thresholds
readonly CPU_THRESHOLD=80
readonly MEMORY_THRESHOLD=80
readonly DISK_THRESHOLD=85

# Notification settings
WEBHOOK_URL="${WEBHOOK_URL:-}"
EMAIL_ALERTS="${EMAIL_ALERTS:-false}"
SLACK_WEBHOOK="${SLACK_WEBHOOK:-}"

# Logging functions
log_info() {
    local msg="$1"
    echo -e "${BLUE}[INFO]${NC} $msg"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [INFO] $msg" >> "$LOG_FILE"
}

log_success() {
    local msg="$1"
    echo -e "${GREEN}[SUCCESS]${NC} $msg"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [SUCCESS] $msg" >> "$LOG_FILE"
}

log_warning() {
    local msg="$1"
    echo -e "${YELLOW}[WARNING]${NC} $msg"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [WARNING] $msg" >> "$LOG_FILE"
}

log_error() {
    local msg="$1"
    echo -e "${RED}[ERROR]${NC} $msg" >&2
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR] $msg" >> "$LOG_FILE"
}

log_header() {
    local msg="$1"
    echo -e "\n${PURPLE}=== $msg ===${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [HEADER] $msg" >> "$LOG_FILE"
}

# Initialize logging
init_logging() {
    mkdir -p "$(dirname "$LOG_FILE")"
    
    # Rotate log file if it's too large (>10MB)
    if [[ -f "$LOG_FILE" ]] && [[ $(stat -f%z "$LOG_FILE" 2>/dev/null || stat -c%s "$LOG_FILE" 2>/dev/null || echo 0) -gt 10485760 ]]; then
        mv "$LOG_FILE" "${LOG_FILE}.old"
        touch "$LOG_FILE"
    fi
}

# Send notification
send_notification() {
    local level="$1"
    local message="$2"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Send to Slack if configured
    if [[ -n "$SLACK_WEBHOOK" ]]; then
        local color="good"
        [[ "$level" == "WARNING" ]] && color="warning"
        [[ "$level" == "ERROR" ]] && color="danger"
        
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"attachments\":[{\"color\":\"$color\",\"title\":\"USDT Platform - $level\",\"text\":\"$message\",\"ts\":\"$(date +%s)\"}]}" \
            "$SLACK_WEBHOOK" 2>/dev/null || true
    fi
    
    # Send to generic webhook if configured
    if [[ -n "$WEBHOOK_URL" ]]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"level\":\"$level\",\"message\":\"$message\",\"timestamp\":\"$timestamp\",\"service\":\"usdt-platform\"}" \
            "$WEBHOOK_URL" 2>/dev/null || true
    fi
}

# Check Docker service status
check_docker_status() {
    log_header "Docker Service Status"
    
    if ! docker info &>/dev/null; then
        log_error "Docker daemon is not running"
        send_notification "ERROR" "Docker daemon is not running"
        return 1
    fi
    
    log_success "Docker daemon is running"
    
    # Check Docker Compose services
    local unhealthy_services=()
    
    for service in "${SERVICES[@]}"; do
        local status
        status=$(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q "$service" | xargs docker inspect --format='{{.State.Status}}' 2>/dev/null || echo "not found")
        
        case "$status" in
            "running")
                log_success "$service: running"
                ;;
            "exited")
                log_error "$service: exited"
                unhealthy_services+=("$service")
                ;;
            "restarting")
                log_warning "$service: restarting"
                ;;
            "not found")
                log_error "$service: container not found"
                unhealthy_services+=("$service")
                ;;
            *)
                log_warning "$service: $status"
                ;;
        esac
    done
    
    if [[ ${#unhealthy_services[@]} -gt 0 ]]; then
        local msg="Unhealthy services detected: ${unhealthy_services[*]}"
        log_error "$msg"
        send_notification "ERROR" "$msg"
        return 1
    fi
    
    return 0
}

# Check service health endpoints
check_health_endpoints() {
    log_header "Service Health Endpoints"
    
    local failed_endpoints=()
    
    for endpoint_config in "${HEALTH_ENDPOINTS[@]}"; do
        local service="${endpoint_config%%:*}"
        local url="${endpoint_config#*:}"
        
        # Skip database connection strings for now
        if [[ "$url" =~ ^(mysql|redis):// ]]; then
            continue
        fi
        
        local response_code
        response_code=$(curl -s -o /dev/null -w "%{http_code}" "$url" --max-time 10 || echo "000")
        
        if [[ "$response_code" =~ ^2[0-9][0-9]$ ]]; then
            log_success "$service health check: HTTP $response_code"
        else
            log_error "$service health check failed: HTTP $response_code"
            failed_endpoints+=("$service")
        fi
    done
    
    if [[ ${#failed_endpoints[@]} -gt 0 ]]; then
        local msg="Health check failures: ${failed_endpoints[*]}"
        log_error "$msg"
        send_notification "WARNING" "$msg"
        return 1
    fi
    
    return 0
}

# Check resource usage
check_resource_usage() {
    log_header "Resource Usage Check"
    
    local alerts=()
    
    # Check system CPU usage
    local cpu_usage
    cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//' | cut -d'%' -f1 || echo "0")
    cpu_usage=${cpu_usage%.*}  # Remove decimal part
    
    if [[ ${cpu_usage:-0} -gt $CPU_THRESHOLD ]]; then
        log_warning "High CPU usage: ${cpu_usage}%"
        alerts+=("CPU: ${cpu_usage}%")
    else
        log_success "CPU usage: ${cpu_usage}%"
    fi
    
    # Check system memory usage
    local memory_usage
    memory_usage=$(free | grep Mem | awk '{printf "%.0f", $3/$2 * 100.0}' || echo "0")
    
    if [[ ${memory_usage:-0} -gt $MEMORY_THRESHOLD ]]; then
        log_warning "High memory usage: ${memory_usage}%"
        alerts+=("Memory: ${memory_usage}%")
    else
        log_success "Memory usage: ${memory_usage}%"
    fi
    
    # Check disk usage
    local disk_usage
    disk_usage=$(df / | awk 'NR==2 {print $5}' | sed 's/%//' || echo "0")
    
    if [[ ${disk_usage:-0} -gt $DISK_THRESHOLD ]]; then
        log_warning "High disk usage: ${disk_usage}%"
        alerts+=("Disk: ${disk_usage}%")
    else
        log_success "Disk usage: ${disk_usage}%"
    fi
    
    # Check Docker container resource usage
    log_info "Docker Container Resource Usage:"
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}" | grep -E "(usdt-|CONTAINER)" || true
    
    if [[ ${#alerts[@]} -gt 0 ]]; then
        local msg="Resource usage alerts: ${alerts[*]}"
        send_notification "WARNING" "$msg"
        return 1
    fi
    
    return 0
}

# Check database connectivity
check_database_connectivity() {
    log_header "Database Connectivity Check"
    
    # Check MySQL connectivity
    if docker exec usdt-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD:-UsdtTrading123!}" -e "SELECT 1;" &>/dev/null; then
        log_success "MySQL connection: OK"
    else
        log_error "MySQL connection: FAILED"
        send_notification "ERROR" "MySQL database connection failed"
        return 1
    fi
    
    # Check Redis connectivity
    if docker exec usdt-redis redis-cli ping | grep -q "PONG"; then
        log_success "Redis connection: OK"
    else
        log_error "Redis connection: FAILED"
        send_notification "ERROR" "Redis cache connection failed"
        return 1
    fi
    
    return 0
}

# Check log files for errors
check_log_files() {
    log_header "Log File Analysis"
    
    local error_count=0
    local warning_count=0
    
    # Check backend logs for errors in the last hour
    if [[ -f "${PROJECT_ROOT}/logs/backend/application.log" ]]; then
        local recent_errors
        recent_errors=$(grep -c "ERROR" "${PROJECT_ROOT}/logs/backend/application.log" | tail -n 100 || echo "0")
        
        if [[ $recent_errors -gt 10 ]]; then
            log_warning "High error count in backend logs: $recent_errors errors"
            error_count=$((error_count + recent_errors))
        fi
    fi
    
    # Check nginx error logs
    if [[ -f "${PROJECT_ROOT}/logs/nginx/error.log" ]]; then
        local nginx_errors
        nginx_errors=$(grep -c "error" "${PROJECT_ROOT}/logs/nginx/error.log" | tail -n 50 || echo "0")
        
        if [[ $nginx_errors -gt 5 ]]; then
            log_warning "Nginx errors detected: $nginx_errors errors"
            error_count=$((error_count + nginx_errors))
        fi
    fi
    
    if [[ $error_count -gt 20 ]]; then
        send_notification "WARNING" "High error count detected in logs: $error_count total errors"
    fi
    
    log_info "Log analysis complete. Total errors: $error_count"
    return 0
}

# Check SSL certificate validity
check_ssl_certificates() {
    log_header "SSL Certificate Check"
    
    local cert_file="${PROJECT_ROOT}/docker/nginx/ssl/cert.pem"
    
    if [[ ! -f "$cert_file" ]]; then
        log_warning "SSL certificate not found: $cert_file"
        return 0
    fi
    
    # Check certificate expiration
    if openssl x509 -in "$cert_file" -checkend 604800 -noout; then  # 7 days
        log_success "SSL certificate is valid"
    else
        log_warning "SSL certificate expires within 7 days"
        send_notification "WARNING" "SSL certificate will expire soon"
    fi
    
    # Get certificate expiration date
    local exp_date
    exp_date=$(openssl x509 -in "$cert_file" -enddate -noout | cut -d= -f2)
    log_info "SSL certificate expires: $exp_date"
    
    return 0
}

# Generate health report
generate_health_report() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local report_file="${PROJECT_ROOT}/logs/health-report-$(date '+%Y%m%d').json"
    
    # Collect system information
    local system_info
    system_info=$(cat << EOF
{
  "timestamp": "$timestamp",
  "system": {
    "uptime": "$(uptime -p || echo 'unknown')",
    "load_average": "$(uptime | awk -F'load average:' '{print $2}' || echo 'unknown')",
    "disk_space": "$(df -h / | awk 'NR==2 {print $4}' || echo 'unknown') available"
  },
  "services": {
EOF
    )
    
    echo "$system_info" > "$report_file"
    
    # Add service status
    local first=true
    for service in "${SERVICES[@]}"; do
        [[ "$first" == "true" ]] && first=false || echo "," >> "$report_file"
        
        local status
        status=$(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q "$service" | xargs docker inspect --format='{{.State.Status}}' 2>/dev/null || echo "unknown")
        
        echo "    \"$service\": \"$status\"" >> "$report_file"
    done
    
    echo "  }" >> "$report_file"
    echo "}" >> "$report_file"
    
    log_info "Health report generated: $report_file"
}

# Main health check function
run_health_check() {
    log_header "USDT Trading Platform Health Check - $(date)"
    
    local exit_code=0
    
    # Run all health checks
    check_docker_status || exit_code=1
    check_health_endpoints || exit_code=1
    check_resource_usage || exit_code=1
    check_database_connectivity || exit_code=1
    check_log_files || exit_code=1
    check_ssl_certificates || exit_code=1
    
    # Generate report
    generate_health_report
    
    if [[ $exit_code -eq 0 ]]; then
        log_success "All health checks passed"
        send_notification "SUCCESS" "All platform services are healthy"
    else
        log_error "Some health checks failed"
        send_notification "ERROR" "Platform health check failures detected"
    fi
    
    log_header "Health Check Complete"
    return $exit_code
}

# Show help
show_help() {
    cat << EOF
Health Check and Monitoring Script for USDT Trading Platform

USAGE:
    $(basename "$0") [COMMAND] [OPTIONS]

COMMANDS:
    check       Run complete health check (default)
    docker      Check Docker services only
    endpoints   Check health endpoints only
    resources   Check resource usage only
    database    Check database connectivity only
    logs        Analyze log files only
    ssl         Check SSL certificates only
    report      Generate health report only
    help        Show this help

ENVIRONMENT VARIABLES:
    WEBHOOK_URL     - Generic webhook URL for notifications
    SLACK_WEBHOOK   - Slack webhook URL for notifications
    EMAIL_ALERTS    - Enable email alerts (true/false)

EXAMPLES:
    $(basename "$0")                    # Run complete health check
    $(basename "$0") docker             # Check Docker services only
    $(basename "$0") resources          # Check resource usage only

SCHEDULING:
    Add to crontab for regular monitoring:
    */5 * * * * $(pwd)/$(basename "$0") >> /var/log/health-check.log 2>&1

EOF
}

# Main function
main() {
    init_logging
    
    case "${1:-check}" in
        "check")
            run_health_check
            ;;
        "docker")
            check_docker_status
            ;;
        "endpoints")
            check_health_endpoints
            ;;
        "resources")
            check_resource_usage
            ;;
        "database")
            check_database_connectivity
            ;;
        "logs")
            check_log_files
            ;;
        "ssl")
            check_ssl_certificates
            ;;
        "report")
            generate_health_report
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

# Execute main function
main "$@"