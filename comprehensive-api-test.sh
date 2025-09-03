#!/bin/bash

# Comprehensive API Testing Script for USDT Trading Platform
# Created by Backend Agent - Master Agent Directive
# Date: $(date)

set -e

# Configuration
BASE_URL="http://localhost:8090"
API_PREFIX="/api/api"
TEST_OUTPUT_DIR="/Users/jason/Projects/usdtClaudeCode/@devdocrecord/BackendAgent"
REPORT_FILE="$TEST_OUTPUT_DIR/comprehensive_api_test_report.md"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test result counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Ensure output directory exists
mkdir -p "$TEST_OUTPUT_DIR"

# Initialize report file
cat > "$REPORT_FILE" << 'EOF'
# USDT Trading Platform - Comprehensive API Test Report

## Executive Summary

**Test Date**: $(date)
**Environment**: Development (localhost:8090)
**Total Endpoints Tested**: TBD
**Passed**: TBD
**Failed**: TBD

---

## Test Results

EOF

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$REPORT_FILE"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1" | tee -a "$REPORT_FILE"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1" | tee -a "$REPORT_FILE"
    ((FAILED_TESTS++))
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$REPORT_FILE"
}

# Test function
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local expected_status="$3"
    local description="$4"
    local data="$5"
    local headers="$6"
    
    ((TOTAL_TESTS++))
    
    log_info "Testing: $description"
    echo "### $description" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Endpoint**: \`$method $endpoint\`" >> "$REPORT_FILE"
    echo "**Expected Status**: $expected_status" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Prepare curl command
    local curl_cmd="curl -s -w \"HTTPSTATUS:%{http_code}\" -X $method \"$BASE_URL$endpoint\" -H \"User-Agent: USDT-API-Test/1.0\""
    
    if [ ! -z "$headers" ]; then
        curl_cmd="$curl_cmd -H \"$headers\""
    fi
    
    if [ ! -z "$data" ]; then
        curl_cmd="$curl_cmd -d '$data' -H 'Content-Type: application/json'"
    fi
    
    # Execute request
    response=$(eval $curl_cmd)
    
    # Extract HTTP status code
    http_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    response_body=$(echo "$response" | sed -E 's/HTTPSTATUS:[0-9]*$//')
    
    echo "**Response Code**: $http_code" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Response Body**:" >> "$REPORT_FILE"
    echo '```json' >> "$REPORT_FILE"
    echo "$response_body" | python3 -m json.tool 2>/dev/null || echo "$response_body" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Check if test passed
    if [ "$http_code" = "$expected_status" ]; then
        log_success "$description - HTTP $http_code"
        echo "**Result**: ✅ PASSED" >> "$REPORT_FILE"
    else
        log_error "$description - Expected HTTP $expected_status, got HTTP $http_code"
        echo "**Result**: ❌ FAILED" >> "$REPORT_FILE"
    fi
    
    echo "" >> "$REPORT_FILE"
    echo "---" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Return response for further processing
    echo "$response_body"
}

# Main test execution
main() {
    log_info "=== Starting Comprehensive API Testing ==="
    
    # Wait for backend to be ready
    log_info "Waiting for backend to be ready..."
    for i in {1..15}; do
        if curl -s -H "User-Agent: USDT-API-Test/1.0" "$BASE_URL$API_PREFIX/test/ping" > /dev/null; then
            log_success "Backend is ready"
            break
        fi
        if [ $i -eq 15 ]; then
            log_error "Backend is not responding after 15 attempts"
            exit 1
        fi
        sleep 2
    done
    
    # 1. Basic API Tests
    log_info "=== 1. Basic API Validation ==="
    test_endpoint "GET" "$API_PREFIX/test/ping" "200" "Basic connectivity test"
    
    # 2. RSA Encryption Tests
    log_info "=== 2. RSA Encryption System ==="
    public_key_response=$(test_endpoint "GET" "$API_PREFIX/admin/auth/public-key" "200" "Get RSA public key")
    
    # 3. Authentication System Tests
    log_info "=== 3. Authentication System ==="
    
    # Test admin login without credentials (should fail)
    test_endpoint "POST" "$API_PREFIX/admin/auth/login" "400" "Admin login without credentials" '{"username":"","password":""}'
    
    # Test admin login with wrong credentials
    test_endpoint "POST" "$API_PREFIX/admin/auth/login" "200" "Admin login with wrong credentials" '{"username":"wrong","password":"wrong"}'
    
    # Test user registration endpoint
    test_endpoint "POST" "$API_PREFIX/auth/register" "400" "User registration without data" '{}'
    
    # 4. Price Module Tests
    log_info "=== 4. Price Module ==="
    test_endpoint "GET" "$API_PREFIX/price/current" "200" "Get current USDT price"
    test_endpoint "GET" "$API_PREFIX/price/history?days=7" "200" "Get 7-day price history"
    
    # 5. Trading System Tests
    log_info "=== 5. Trading System ==="
    test_endpoint "GET" "$API_PREFIX/trading/markets" "200" "Get available markets"
    
    # 6. Wallet Tests
    log_info "=== 6. Wallet System ==="
    test_endpoint "GET" "$API_PREFIX/wallet/balance" "401" "Get wallet balance (no auth)"
    
    # 7. Error Handling Tests
    log_info "=== 7. Error Handling ==="
    test_endpoint "GET" "$API_PREFIX/nonexistent" "404" "Non-existent endpoint"
    test_endpoint "POST" "$API_PREFIX/test/simulate-error?errorCode=400" "200" "Simulate 400 error"
    test_endpoint "POST" "$API_PREFIX/test/simulate-error?errorCode=500" "200" "Simulate 500 error"
    
    # 8. CORS Tests
    log_info "=== 8. CORS Configuration ==="
    test_endpoint "GET" "$API_PREFIX/test/cors-test" "200" "CORS configuration test"
    
    # 9. Session and Security Tests
    log_info "=== 9. Session and Security ==="
    test_endpoint "GET" "$API_PREFIX/admin/auth/session/validate" "200" "Session validation without auth"
    
    # Generate final summary
    log_info "=== Test Summary ==="
    log_info "Total Tests: $TOTAL_TESTS"
    log_info "Passed: $PASSED_TESTS"
    log_info "Failed: $FAILED_TESTS"
    
    # Update report summary
    sed -i.bak "s/TBD/$TOTAL_TESTS/g; s/Passed: TBD/Passed: $PASSED_TESTS/; s/Failed: TBD/Failed: $FAILED_TESTS/" "$REPORT_FILE" 2>/dev/null || true
    
    # Add summary to report
    cat >> "$REPORT_FILE" << EOF

## Final Summary

- **Total Tests**: $TOTAL_TESTS
- **Passed**: $PASSED_TESTS
- **Failed**: $FAILED_TESTS
- **Success Rate**: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%

## Recommendations

Based on the test results:

1. **Critical Issues**: Any endpoints returning unexpected error codes
2. **Security Concerns**: Check authentication and authorization implementations
3. **Performance**: Monitor response times for optimization opportunities
4. **Documentation**: Update API documentation based on actual behavior

## Technical Analysis

### Authentication Flow
- RSA public key endpoint accessibility
- Login/logout functionality
- Session management

### Error Handling
- Consistent error response format
- Appropriate HTTP status codes
- Meaningful error messages

### Security Implementation
- CORS configuration
- Input validation
- Authentication requirements

---

*Report generated by Backend Agent as part of Master Agent directive*
*Generation time: $(date)*
EOF

    if [ $FAILED_TESTS -gt 0 ]; then
        log_warning "Some tests failed. Check the detailed report at: $REPORT_FILE"
        exit 1
    else
        log_success "All tests passed! Full report available at: $REPORT_FILE"
        exit 0
    fi
}

# Run main function
main "$@"