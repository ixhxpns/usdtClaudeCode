#!/bin/bash
# ===========================================
# RSA Key Issue Fix Script
# ===========================================
# This script fixes the RSA public key fetch failure
# by generating proper RSA keys and restarting services

set -e

echo "🔧 Starting RSA Key Issue Fix..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if OpenSSL is available
if ! command -v openssl &> /dev/null; then
    print_error "OpenSSL is not installed. Please install it first."
    exit 1
fi

print_status "Checking current RSA configuration..."

# Create keys directory if it doesn't exist
mkdir -p ./keys

# Generate RSA private key
print_status "Generating RSA private key (2048 bits)..."
openssl genrsa -out ./keys/private_key.pem 2048

# Extract public key
print_status "Extracting RSA public key..."
openssl rsa -in ./keys/private_key.pem -pubout -out ./keys/public_key.pem

# Convert to Base64 (remove headers and newlines)
print_status "Converting keys to Base64 format..."
PUBLIC_KEY_BASE64=$(openssl rsa -in ./keys/private_key.pem -pubout -outform DER | base64 | tr -d '\n')
PRIVATE_KEY_BASE64=$(openssl rsa -in ./keys/private_key.pem -outform DER | base64 | tr -d '\n')

# Update .env file
print_status "Updating .env file with new RSA keys..."
if [ -f .env ]; then
    # Backup existing .env
    cp .env .env.backup
    print_status "Created backup: .env.backup"
    
    # Update RSA keys in .env file
    sed -i.tmp "s|^RSA_PUBLIC_KEY=.*|RSA_PUBLIC_KEY=${PUBLIC_KEY_BASE64}|g" .env
    sed -i.tmp "s|^RSA_PRIVATE_KEY=.*|RSA_PRIVATE_KEY=${PRIVATE_KEY_BASE64}|g" .env
    rm .env.tmp
    
    print_success "Updated RSA keys in .env file"
else
    print_error ".env file not found!"
    exit 1
fi

# Set proper permissions for key files
chmod 600 ./keys/private_key.pem
chmod 644 ./keys/public_key.pem

print_success "RSA keys generated and configured successfully!"

# Display key information
echo
print_status "Key Information:"
echo "  Private Key: ./keys/private_key.pem"
echo "  Public Key:  ./keys/public_key.pem"
echo "  Key Size:    2048 bits"

# Check Docker services
print_status "Checking Docker services status..."
if docker-compose ps | grep -q "usdt-backend"; then
    print_status "Restarting backend service to apply new RSA configuration..."
    docker-compose restart backend
    
    # Wait for backend to be healthy
    print_status "Waiting for backend to be ready..."
    sleep 10
    
    # Test the RSA endpoint
    print_status "Testing RSA public key endpoint..."
    if curl -s -f "http://localhost:8090/api/auth/public-key" > /dev/null; then
        print_success "RSA public key endpoint is now working!"
    else
        print_warning "RSA endpoint still not accessible. You may need to rebuild the backend."
        echo
        print_status "To rebuild the backend:"
        echo "  docker-compose down backend"
        echo "  docker-compose build --no-cache backend"
        echo "  docker-compose up -d backend"
    fi
else
    print_warning "Backend service is not running. Please start it with: docker-compose up -d"
fi

echo
print_success "RSA key fix completed!"
print_warning "IMPORTANT: These keys are for development only. Generate new keys for production!"

# Security reminder
echo
print_status "Security Reminders:"
echo "  1. Never commit private keys to version control"
echo "  2. Generate new keys for production deployment"
echo "  3. Store production keys in secure key management systems"
echo "  4. Rotate keys regularly in production"

exit 0