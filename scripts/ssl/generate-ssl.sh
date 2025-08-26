#!/bin/bash

# SSL Certificate Generation Script for USDT Trading Platform
# This script generates SSL certificates for development and production environments

set -euo pipefail

# Color codes
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

# Configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly SSL_DIR="${PROJECT_ROOT}/docker/nginx/ssl"
readonly CERTS_DIR="${SSL_DIR}/certs"
readonly PRIVATE_DIR="${SSL_DIR}/private"

# Default values
DEFAULT_DOMAIN="localhost"
DEFAULT_ORG="USDT Trading Platform"
DEFAULT_COUNTRY="US"
DEFAULT_STATE="State"
DEFAULT_CITY="City"
DEFAULT_EMAIL="admin@localhost"

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
    echo -e "\n${BLUE}=== $1 ===${NC}"
}

# Check dependencies
check_dependencies() {
    local deps=("openssl")
    
    for dep in "${deps[@]}"; do
        if ! command -v "$dep" &> /dev/null; then
            log_error "$dep is required but not installed"
            exit 1
        fi
    done
}

# Create directories
create_directories() {
    log_info "Creating SSL directories..."
    
    mkdir -p "$SSL_DIR" "$CERTS_DIR" "$PRIVATE_DIR"
    
    # Set secure permissions
    chmod 700 "$PRIVATE_DIR"
    chmod 755 "$SSL_DIR" "$CERTS_DIR"
    
    log_success "SSL directories created"
}

# Generate CA certificate
generate_ca() {
    local ca_key="$PRIVATE_DIR/ca.key"
    local ca_cert="$CERTS_DIR/ca.crt"
    
    log_header "Generating Certificate Authority"
    
    if [[ -f "$ca_cert" ]]; then
        log_warning "CA certificate already exists. Skipping..."
        return 0
    fi
    
    # Generate CA private key
    openssl genrsa -out "$ca_key" 4096
    chmod 600 "$ca_key"
    
    # Generate CA certificate
    openssl req -new -x509 -days 3650 -key "$ca_key" -out "$ca_cert" \
        -subj "/C=${COUNTRY:-$DEFAULT_COUNTRY}/ST=${STATE:-$DEFAULT_STATE}/L=${CITY:-$DEFAULT_CITY}/O=${ORG:-$DEFAULT_ORG}/CN=${ORG:-$DEFAULT_ORG} CA"
    
    log_success "CA certificate generated: $ca_cert"
}

# Generate self-signed certificate
generate_self_signed() {
    local domain="${1:-$DEFAULT_DOMAIN}"
    local cert_key="$PRIVATE_DIR/${domain}.key"
    local cert_file="$CERTS_DIR/${domain}.crt"
    
    log_header "Generating Self-Signed Certificate for $domain"
    
    # Create OpenSSL config for SAN
    local config_file="/tmp/openssl-${domain}.cnf"
    cat > "$config_file" << EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = v3_req

[dn]
C=${COUNTRY:-$DEFAULT_COUNTRY}
ST=${STATE:-$DEFAULT_STATE}
L=${CITY:-$DEFAULT_CITY}
O=${ORG:-$DEFAULT_ORG}
CN=${domain}

[v3_req]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = ${domain}
DNS.2 = www.${domain}
DNS.3 = api.${domain}
DNS.4 = admin.${domain}
IP.1 = 127.0.0.1
IP.2 = ::1
EOF
    
    # Generate private key
    openssl genrsa -out "$cert_key" 2048
    chmod 600 "$cert_key"
    
    # Generate certificate signing request
    local csr_file="/tmp/${domain}.csr"
    openssl req -new -key "$cert_key" -out "$csr_file" -config "$config_file"
    
    # Generate self-signed certificate
    openssl x509 -req -days 365 -in "$csr_file" -signkey "$cert_key" \
        -out "$cert_file" -extensions v3_req -extfile "$config_file"
    
    # Clean up temporary files
    rm -f "$config_file" "$csr_file"
    
    log_success "Self-signed certificate generated: $cert_file"
}

# Generate CA-signed certificate
generate_ca_signed() {
    local domain="${1:-$DEFAULT_DOMAIN}"
    local ca_key="$PRIVATE_DIR/ca.key"
    local ca_cert="$CERTS_DIR/ca.crt"
    local cert_key="$PRIVATE_DIR/${domain}.key"
    local cert_file="$CERTS_DIR/${domain}.crt"
    
    log_header "Generating CA-Signed Certificate for $domain"
    
    if [[ ! -f "$ca_cert" ]]; then
        log_error "CA certificate not found. Please generate CA first."
        return 1
    fi
    
    # Create OpenSSL config for SAN
    local config_file="/tmp/openssl-${domain}.cnf"
    cat > "$config_file" << EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = v3_req

[dn]
C=${COUNTRY:-$DEFAULT_COUNTRY}
ST=${STATE:-$DEFAULT_STATE}
L=${CITY:-$DEFAULT_CITY}
O=${ORG:-$DEFAULT_ORG}
CN=${domain}

[v3_req]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = ${domain}
DNS.2 = www.${domain}
DNS.3 = api.${domain}
DNS.4 = admin.${domain}
IP.1 = 127.0.0.1
IP.2 = ::1
EOF
    
    # Generate private key
    openssl genrsa -out "$cert_key" 2048
    chmod 600 "$cert_key"
    
    # Generate certificate signing request
    local csr_file="/tmp/${domain}.csr"
    openssl req -new -key "$cert_key" -out "$csr_file" -config "$config_file"
    
    # Sign certificate with CA
    openssl x509 -req -days 365 -in "$csr_file" -CA "$ca_cert" -CAkey "$ca_key" \
        -CAcreateserial -out "$cert_file" -extensions v3_req -extfile "$config_file"
    
    # Clean up temporary files
    rm -f "$config_file" "$csr_file"
    
    log_success "CA-signed certificate generated: $cert_file"
}

# Setup Let's Encrypt certificates
setup_letsencrypt() {
    local domain="${1:-}"
    local email="${2:-$DEFAULT_EMAIL}"
    
    if [[ -z "$domain" ]]; then
        log_error "Domain is required for Let's Encrypt"
        return 1
    fi
    
    log_header "Setting up Let's Encrypt Certificate for $domain"
    
    # Check if certbot is available
    if ! command -v certbot &> /dev/null; then
        log_error "certbot is required for Let's Encrypt certificates"
        log_info "Please install certbot: apt-get install certbot python3-certbot-nginx"
        return 1
    fi
    
    # Create webroot directory
    local webroot_dir="${PROJECT_ROOT}/data/letsencrypt"
    mkdir -p "$webroot_dir"
    
    # Generate certificate
    certbot certonly \
        --webroot \
        --webroot-path "$webroot_dir" \
        --email "$email" \
        --agree-tos \
        --no-eff-email \
        --domains "$domain,www.$domain,api.$domain,admin.$domain"
    
    # Copy certificates to SSL directory
    local le_cert_dir="/etc/letsencrypt/live/$domain"
    if [[ -d "$le_cert_dir" ]]; then
        cp "$le_cert_dir/fullchain.pem" "$SSL_DIR/cert.pem"
        cp "$le_cert_dir/privkey.pem" "$SSL_DIR/key.pem"
        chmod 644 "$SSL_DIR/cert.pem"
        chmod 600 "$SSL_DIR/key.pem"
        
        log_success "Let's Encrypt certificate installed"
    else
        log_error "Let's Encrypt certificate generation failed"
        return 1
    fi
}

# Create default certificates for development
create_default_certs() {
    log_header "Creating Default Development Certificates"
    
    # Create cert.pem and key.pem for nginx
    local default_cert="$SSL_DIR/cert.pem"
    local default_key="$SSL_DIR/key.pem"
    
    if [[ -f "$default_cert" ]] && [[ -f "$default_key" ]]; then
        log_warning "Default certificates already exist"
        return 0
    fi
    
    # Generate self-signed certificate for localhost
    generate_self_signed "localhost"
    
    # Create symlinks for nginx
    ln -sf "$CERTS_DIR/localhost.crt" "$default_cert"
    ln -sf "$PRIVATE_DIR/localhost.key" "$default_key"
    
    log_success "Default development certificates created"
}

# Validate certificate
validate_certificate() {
    local cert_file="${1:-$SSL_DIR/cert.pem}"
    
    if [[ ! -f "$cert_file" ]]; then
        log_error "Certificate file not found: $cert_file"
        return 1
    fi
    
    log_header "Certificate Information"
    
    # Display certificate details
    openssl x509 -in "$cert_file" -text -noout | grep -E "(Subject|Issuer|Not Before|Not After|DNS|IP Address)"
    
    # Check certificate validity
    if openssl x509 -in "$cert_file" -checkend 86400 -noout; then
        log_success "Certificate is valid for at least 24 hours"
    else
        log_warning "Certificate will expire within 24 hours"
    fi
}

# Setup certificate renewal
setup_renewal() {
    log_header "Setting up Certificate Renewal"
    
    # Create renewal script
    local renewal_script="${PROJECT_ROOT}/scripts/ssl/renew-ssl.sh"
    cat > "$renewal_script" << 'EOF'
#!/bin/bash
# SSL Certificate Renewal Script

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SSL_DIR="${PROJECT_ROOT}/docker/nginx/ssl"

# Function to log messages
log_info() {
    echo "[$(date)] [INFO] $1"
}

log_error() {
    echo "[$(date)] [ERROR] $1" >&2
}

# Renew Let's Encrypt certificates
if command -v certbot &> /dev/null; then
    log_info "Renewing Let's Encrypt certificates..."
    
    if certbot renew --quiet; then
        log_info "Certificates renewed successfully"
        
        # Reload nginx in docker container
        if docker ps | grep -q "usdt-nginx"; then
            docker exec usdt-nginx nginx -s reload
            log_info "Nginx reloaded"
        fi
    else
        log_error "Certificate renewal failed"
        exit 1
    fi
else
    log_info "Certbot not found, skipping renewal"
fi
EOF
    
    chmod +x "$renewal_script"
    
    # Create systemd timer for automatic renewal (if systemd is available)
    if systemctl --version &> /dev/null; then
        log_info "Creating systemd timer for certificate renewal..."
        
        # Create service file
        sudo tee /etc/systemd/system/ssl-renewal.service > /dev/null << EOF
[Unit]
Description=Renew SSL certificates
After=network-online.target

[Service]
Type=oneshot
ExecStart=${renewal_script}
User=$(whoami)
EOF
        
        # Create timer file
        sudo tee /etc/systemd/system/ssl-renewal.timer > /dev/null << EOF
[Unit]
Description=Renew SSL certificates daily
Requires=ssl-renewal.service

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
EOF
        
        # Enable and start timer
        sudo systemctl daemon-reload
        sudo systemctl enable ssl-renewal.timer
        sudo systemctl start ssl-renewal.timer
        
        log_success "Systemd timer created for daily certificate renewal"
    else
        log_info "Add the following cron job for certificate renewal:"
        log_info "0 2 * * * ${renewal_script} >> /var/log/ssl-renewal.log 2>&1"
    fi
}

# Show help
show_help() {
    cat << EOF
SSL Certificate Generation Script for USDT Trading Platform

USAGE:
    $(basename "$0") [COMMAND] [OPTIONS]

COMMANDS:
    ca                      Generate Certificate Authority
    self-signed [domain]    Generate self-signed certificate
    ca-signed [domain]      Generate CA-signed certificate
    letsencrypt <domain>    Setup Let's Encrypt certificate
    default                 Create default development certificates
    validate [cert_file]    Validate certificate
    renewal                 Setup certificate renewal
    help                    Show this help

OPTIONS:
    Environment variables can be used to customize certificate details:
    DOMAIN      - Domain name (default: localhost)
    ORG         - Organization name (default: USDT Trading Platform)
    COUNTRY     - Country code (default: US)
    STATE       - State/Province (default: State)
    CITY        - City/Locality (default: City)
    EMAIL       - Email address (default: admin@localhost)

EXAMPLES:
    $(basename "$0") default                           # Create development certificates
    $(basename "$0") self-signed example.com          # Generate self-signed cert
    $(basename "$0") letsencrypt yourdomain.com       # Setup Let's Encrypt
    $(basename "$0") validate                         # Validate default certificate

FILES:
    SSL certificates are stored in: ${SSL_DIR}
    Default certificates: cert.pem, key.pem

EOF
}

# Main function
main() {
    check_dependencies
    create_directories
    
    case "${1:-default}" in
        "ca")
            generate_ca
            ;;
        "self-signed")
            generate_self_signed "${2:-$DEFAULT_DOMAIN}"
            ;;
        "ca-signed")
            generate_ca_signed "${2:-$DEFAULT_DOMAIN}"
            ;;
        "letsencrypt")
            if [[ -z "${2:-}" ]]; then
                log_error "Domain is required for Let's Encrypt"
                exit 1
            fi
            setup_letsencrypt "$2" "${EMAIL:-$DEFAULT_EMAIL}"
            ;;
        "default")
            create_default_certs
            ;;
        "validate")
            validate_certificate "${2:-}"
            ;;
        "renewal")
            setup_renewal
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