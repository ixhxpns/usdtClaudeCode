#!/bin/bash
# ===========================================
# Network Connectivity Diagnostic Script
# ===========================================
# This script diagnoses network connectivity issues
# between frontend, backend, and proxy services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to print colored output
print_header() {
    echo -e "\n${CYAN}=== $1 ===${NC}"
}

print_status() {
    echo -e "${BLUE}[CHECK]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

print_info() {
    echo -e "${CYAN}[INFO]${NC} $1"
}

# Test HTTP endpoint
test_endpoint() {
    local url=$1
    local description=$2
    local expected_status=${3:-200}
    
    print_status "Testing: $description"
    echo "  URL: $url"
    
    if response=$(curl -s -w "HTTPSTATUS:%{http_code};TIME:%{time_total}" "$url" 2>/dev/null); then
        http_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
        time_total=$(echo "$response" | grep -o "TIME:[0-9.]*" | cut -d: -f2)
        
        if [ "$http_code" = "$expected_status" ]; then
            print_success "✓ HTTP $http_code (${time_total}s)"
        else
            print_warning "✗ HTTP $http_code (expected $expected_status) (${time_total}s)"
        fi
        return $http_code
    else
        print_error "✗ Connection failed"
        return 999
    fi
}

# Test TCP port
test_port() {
    local host=$1
    local port=$2
    local description=$3
    
    print_status "Testing: $description"
    echo "  Host: $host:$port"
    
    if timeout 5 bash -c "echo >/dev/tcp/$host/$port" 2>/dev/null; then
        print_success "✓ Port $port is open"
        return 0
    else
        print_error "✗ Port $port is closed or filtered"
        return 1
    fi
}

# Main diagnostic function
main() {
    echo -e "${CYAN}"
    echo "██╗   ██╗███████╗██████╗ ████████╗    ███╗   ██╗███████╗████████╗"
    echo "██║   ██║██╔════╝██╔══██╗╚══██╔══╝    ████╗  ██║██╔════╝╚══██╔══╝"
    echo "██║   ██║███████╗██║  ██║   ██║       ██╔██╗ ██║█████╗     ██║   "
    echo "██║   ██║╚════██║██║  ██║   ██║       ██║╚██╗██║██╔══╝     ██║   "
    echo "╚██████╔╝███████║██████╔╝   ██║       ██║ ╚████║███████╗   ██║   "
    echo " ╚═════╝ ╚══════╝╚═════╝    ╚═╝       ╚═╝  ╚═══╝╚══════╝   ╚═╝   "
    echo -e "${NC}"
    echo "Network Diagnostic Tool for USDT Trading Platform"
    echo "=================================================="
    
    print_header "Docker Services Status"
    if command -v docker-compose &> /dev/null; then
        docker-compose ps
        echo
        
        # Check if services are running
        if docker-compose ps | grep -q "usdt-backend.*Up"; then
            print_success "Backend service is running"
        else
            print_error "Backend service is not running"
        fi
        
        if docker-compose ps | grep -q "usdt-admin-frontend.*Up"; then
            print_success "Admin frontend service is running"
        else
            print_error "Admin frontend service is not running"
        fi
        
        if docker-compose ps | grep -q "usdt-nginx.*Up"; then
            print_success "Nginx proxy service is running"
        else
            print_error "Nginx proxy service is not running"
        fi
    else
        print_error "Docker Compose not found"
    fi
    
    print_header "Port Connectivity Tests"
    test_port "localhost" "3000" "Admin Frontend (Direct)"
    test_port "localhost" "3001" "User Frontend (Direct)"
    test_port "localhost" "8090" "Backend API (Direct)"
    test_port "localhost" "80" "Nginx HTTP"
    test_port "localhost" "443" "Nginx HTTPS"
    test_port "localhost" "3306" "MySQL Database"
    test_port "localhost" "6379" "Redis Cache"
    
    print_header "HTTP Endpoint Tests"
    
    # Backend API Tests
    test_endpoint "http://localhost:8090/api/actuator/health" "Backend Health Check"
    test_endpoint "http://localhost:8090/api/auth/public-key" "RSA Public Key (Direct)"
    
    # Frontend Tests  
    test_endpoint "http://localhost:3000/" "Admin Frontend (Direct)"
    test_endpoint "http://localhost:3001/" "User Frontend (Direct)"
    
    # Nginx Proxy Tests
    test_endpoint "http://localhost:80/health" "Nginx Health Check"
    test_endpoint "http://localhost:80/api/actuator/health" "Backend via Nginx"
    
    print_header "API Response Analysis"
    
    print_status "Checking RSA Public Key API Response..."
    if response=$(curl -s "http://localhost:8090/api/auth/public-key" 2>/dev/null); then
        if echo "$response" | jq . &>/dev/null; then
            print_success "Valid JSON response received"
            if echo "$response" | jq -e '.success' &>/dev/null; then
                print_success "API reports success: true"
                if echo "$response" | jq -e '.data.publicKey' &>/dev/null; then
                    key_length=$(echo "$response" | jq -r '.data.publicKey' | wc -c)
                    print_success "RSA public key present (${key_length} chars)"
                else
                    print_error "No publicKey field in response"
                fi
            else
                print_error "API reports success: false"
                error_msg=$(echo "$response" | jq -r '.message // "Unknown error"')
                print_info "Error message: $error_msg"
            fi
        else
            print_error "Invalid JSON response"
            print_info "Response preview: $(echo "$response" | head -c 200)..."
        fi
    else
        print_error "No response from RSA public key endpoint"
    fi
    
    print_header "Environment Configuration Check"
    
    if [ -f ".env" ]; then
        print_success ".env file exists"
        
        # Check critical environment variables
        if grep -q "^RSA_PUBLIC_KEY=" .env && [ -n "$(grep "^RSA_PUBLIC_KEY=" .env | cut -d= -f2)" ]; then
            print_success "RSA_PUBLIC_KEY is configured"
        else
            print_error "RSA_PUBLIC_KEY is not configured"
        fi
        
        if grep -q "^RSA_PRIVATE_KEY=" .env && [ -n "$(grep "^RSA_PRIVATE_KEY=" .env | cut -d= -f2)" ]; then
            print_success "RSA_PRIVATE_KEY is configured"
        else
            print_warning "RSA_PRIVATE_KEY is not configured (may be optional)"
        fi
        
        if grep -q "^JWT_SECRET_KEY=" .env && [ -n "$(grep "^JWT_SECRET_KEY=" .env | cut -d= -f2)" ]; then
            print_success "JWT_SECRET_KEY is configured"
        else
            print_warning "JWT_SECRET_KEY is not configured"
        fi
    else
        print_error ".env file not found"
    fi
    
    print_header "Docker Network Analysis"
    
    # Check Docker networks
    if docker network ls | grep -q "usdt-network"; then
        print_success "usdt-network exists"
        
        # Show network details
        print_info "Network containers:"
        docker network inspect usdt-network --format '{{range .Containers}}  - {{.Name}} ({{.IPv4Address}}){{end}}' 2>/dev/null || true
    else
        print_warning "usdt-network not found"
    fi
    
    print_header "CORS and Proxy Configuration"
    
    print_status "Checking Vite proxy configuration..."
    if [ -f "frontend/admin/vite.config.ts" ]; then
        if grep -q "proxy:" "frontend/admin/vite.config.ts"; then
            target=$(grep -A 5 "proxy:" "frontend/admin/vite.config.ts" | grep "target:" | head -1)
            print_success "Proxy configured: $target"
        else
            print_warning "No proxy configuration found in Vite config"
        fi
    fi
    
    print_status "Checking Nginx proxy configuration..."
    if [ -f "docker/nginx/conf.d/default.conf" ]; then
        if grep -q "proxy_pass.*backend" "docker/nginx/conf.d/default.conf"; then
            print_success "Nginx backend proxy configured"
        else
            print_warning "No backend proxy found in Nginx config"
        fi
    fi
    
    print_header "Recommendations"
    
    print_info "Common fixes for RSA key issues:"
    echo "  1. Run: ./scripts/fix-rsa-key-issue.sh"
    echo "  2. Restart services: docker-compose restart backend"
    echo "  3. Check logs: docker logs usdt-backend"
    echo "  4. Rebuild if needed: docker-compose build --no-cache backend"
    
    print_info "Network debugging commands:"
    echo "  - Check container logs: docker logs <container_name>"
    echo "  - Test internal connectivity: docker exec <container> curl <url>"
    echo "  - Inspect networks: docker network inspect usdt-network"
    echo "  - Monitor network traffic: docker stats"
    
    echo
    print_success "Network diagnostic completed!"
}

# Run main function
main "$@"