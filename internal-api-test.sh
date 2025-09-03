#!/bin/bash

# Internal API Testing Script - runs inside Docker container
# Created by Backend Agent for Master Agent directive

set -e

# Configuration
BASE_URL="http://localhost:8090"
API_PREFIX="/api/api"
USER_AGENT="USDT-API-Test/1.0"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((FAILED_TESTS++))
}

# Test function
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local expected_status="$3"
    local description="$4"
    local data="$5"
    
    ((TOTAL_TESTS++))
    
    log_info "Testing: $description"
    
    # Prepare curl command
    local curl_cmd="curl -s -w \"HTTPSTATUS:%{http_code}\" -X $method -H \"User-Agent: $USER_AGENT\" \"$BASE_URL$endpoint\""
    
    if [ ! -z "$data" ]; then
        curl_cmd="$curl_cmd -d '$data' -H 'Content-Type: application/json'"
    fi
    
    # Execute request
    response=$(eval $curl_cmd)
    
    # Extract HTTP status code
    http_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    response_body=$(echo "$response" | sed -E 's/HTTPSTATUS:[0-9]*$//')
    
    echo "  Response Code: $http_code"
    echo "  Response Body: $response_body"
    
    # Check if test passed
    if [ "$http_code" = "$expected_status" ]; then
        log_success "$description - HTTP $http_code"
    else
        log_error "$description - Expected HTTP $expected_status, got HTTP $http_code"
    fi
    
    echo ""
}

# Main test execution
main() {
    log_info "=== Starting Internal API Testing ==="
    
    # 1. Basic API Tests
    log_info "=== 1. Basic API Validation ==="
    test_endpoint "GET" "$API_PREFIX/test/ping" "200" "Basic connectivity test"
    test_endpoint "GET" "$API_PREFIX/test/cors-test" "200" "CORS test"
    
    # 2. RSA Encryption Tests
    log_info "=== 2. RSA Encryption System ==="
    test_endpoint "GET" "$API_PREFIX/admin/auth/public-key" "200" "Get RSA public key"
    
    # 3. Authentication System Tests
    log_info "=== 3. Authentication System ==="
    test_endpoint "POST" "$API_PREFIX/admin/auth/login" "200" "Admin login without credentials" '{"username":"","password":""}'
    test_endpoint "GET" "$API_PREFIX/admin/auth/session/validate" "200" "Session validation without auth"
    
    # 4. Price Module Tests
    log_info "=== 4. Price Module ==="
    test_endpoint "GET" "$API_PREFIX/price/current" "200" "Get current USDT price"
    test_endpoint "GET" "$API_PREFIX/price/history?days=7" "200" "Get 7-day price history"
    
    # 5. Error Handling Tests
    log_info "=== 5. Error Handling ==="
    test_endpoint "GET" "$API_PREFIX/nonexistent" "404" "Non-existent endpoint"
    test_endpoint "POST" "$API_PREFIX/test/simulate-error?errorCode=400" "200" "Simulate 400 error"
    
    # Generate summary
    log_info "=== Test Summary ==="
    log_info "Total Tests: $TOTAL_TESTS"
    log_info "Passed: $PASSED_TESTS"
    log_info "Failed: $FAILED_TESTS"
    
    success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
    log_info "Success Rate: $success_rate%"
    
    return $(( TOTAL_TESTS - PASSED_TESTS ))
}

# Run main function
main "$@"