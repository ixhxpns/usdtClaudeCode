# USDT Trading Platform - Docker Deployment Guide

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [SSL Certificates](#ssl-certificates)
- [Monitoring & Health Checks](#monitoring--health-checks)
- [Logging](#logging)
- [Backup & Recovery](#backup--recovery)
- [Security](#security)
- [Troubleshooting](#troubleshooting)
- [Production Deployment](#production-deployment)
- [Maintenance](#maintenance)

## Overview

This deployment guide covers the complete Docker-based deployment of the USDT Trading Platform, including:

- **Backend API** (Spring Boot + Java 17)
- **Admin Frontend** (Vue.js + Nginx)
- **User Frontend** (Vue.js + Nginx)
- **MySQL Database** (8.0)
- **Redis Cache** (7.2)
- **Nginx Reverse Proxy** (1.25)

### Architecture

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│    User     │    │   Admin     │    │   Mobile    │
│  Frontend   │    │  Frontend   │    │     App     │
└─────────────┘    └─────────────┘    └─────────────┘
       │                    │                    │
       └─────────────────┬──┘                    │
                         │                       │
                ┌─────────▼─────────┐             │
                │      Nginx        │             │
                │  Reverse Proxy    │             │
                └─────────┬─────────┘             │
                          │                       │
                ┌─────────▼─────────┐             │
                │    Backend API    │◄────────────┘
                │   (Spring Boot)   │
                └─────────┬─────────┘
                          │
          ┌───────────────┼───────────────┐
          │               │               │
    ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐
    │   MySQL   │  │   Redis   │  │   Files   │
    │ Database  │  │   Cache   │  │ Storage   │
    └───────────┘  └───────────┘  └───────────┘
```

## Prerequisites

### System Requirements

- **Operating System**: Linux (Ubuntu 20.04+ recommended) or macOS
- **CPU**: Minimum 2 cores, Recommended 4+ cores
- **Memory**: Minimum 4GB RAM, Recommended 8GB+ RAM
- **Storage**: Minimum 20GB free space, Recommended 50GB+ for production
- **Network**: Stable internet connection

### Software Dependencies

```bash
# Docker Engine 20.10+
sudo apt-get install docker.io

# Docker Compose 2.0+
sudo apt-get install docker-compose-plugin

# OpenSSL (for SSL certificates)
sudo apt-get install openssl

# Curl (for health checks)
sudo apt-get install curl

# Git (for version control)
sudo apt-get install git
```

### Verify Installation

```bash
# Check Docker version
docker --version
docker-compose --version

# Check if Docker daemon is running
docker info

# Verify Docker Compose
docker-compose version
```

## Quick Start

### 1. Clone Repository

```bash
git clone <repository-url>
cd usdtClaudeCode
```

### 2. Environment Setup

```bash
# Copy environment template
cp .env.example .env

# Edit configuration (see Configuration section)
nano .env
```

### 3. Start Platform

```bash
# Make start script executable
chmod +x start.sh

# Start all services
./start.sh start
```

### 4. Verify Deployment

```bash
# Check service status
./start.sh status

# View logs
./start.sh logs

# Access applications
# User Frontend: https://localhost/user/
# Admin Frontend: https://localhost/admin/
# API Documentation: https://localhost/api/swagger-ui.html
```

## Configuration

### Environment Variables

The `.env` file contains all configuration options. Key sections:

#### Database Configuration

```bash
# MySQL Database
MYSQL_ROOT_PASSWORD=your-secure-root-password
MYSQL_USER=usdt_user
MYSQL_PASSWORD=your-secure-user-password
MYSQL_DATABASE=usdt_trading_platform
```

#### Redis Configuration

```bash
# Redis Cache
REDIS_PASSWORD=your-redis-password  # Optional
REDIS_HOST=redis
REDIS_PORT=6379
```

#### JWT Configuration

```bash
# JWT Authentication
JWT_SECRET_KEY=your-256-bit-secret-key-change-this-in-production
JWT_EXPIRATION=86400  # 24 hours
```

#### Email Configuration

```bash
# SMTP Settings
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

#### Blockchain Configuration

```bash
# Ethereum/USDT Integration
BLOCKCHAIN_NODE_URL=https://mainnet.infura.io/v3/your-project-id
BLOCKCHAIN_PRIVATE_KEY=your-private-key
USDT_CONTRACT_ADDRESS=0xdAC17F958D2ee523a2206206994597C13D831ec7
```

### Port Configuration

Default port mappings:

| Service | Internal Port | External Port | Description |
|---------|---------------|---------------|-------------|
| Nginx | 80/443 | 80/443 | Web server |
| Backend | 8080 | 8080 | API server |
| User Frontend | 80 | 3001 | User interface |
| Admin Frontend | 80 | 3000 | Admin interface |
| MySQL | 3306 | 3306 | Database |
| Redis | 6379 | 6379 | Cache |

### Resource Limits

Configure resource limits in `.env`:

```bash
# Memory limits
MYSQL_MEMORY_LIMIT=1g
REDIS_MEMORY_LIMIT=512m
BACKEND_MEMORY_LIMIT=2g
FRONTEND_MEMORY_LIMIT=256m
NGINX_MEMORY_LIMIT=256m

# CPU limits
MYSQL_CPU_LIMIT=1.0
REDIS_CPU_LIMIT=0.5
BACKEND_CPU_LIMIT=1.0
FRONTEND_CPU_LIMIT=0.5
NGINX_CPU_LIMIT=0.5
```

## SSL Certificates

### Development Environment

For development, self-signed certificates are automatically generated:

```bash
# Generate development certificates
./scripts/ssl/generate-ssl.sh default

# Validate certificates
./scripts/ssl/generate-ssl.sh validate
```

### Production Environment

#### Option 1: Let's Encrypt (Recommended)

```bash
# Install Certbot
sudo apt-get install certbot python3-certbot-nginx

# Generate Let's Encrypt certificate
./scripts/ssl/generate-ssl.sh letsencrypt yourdomain.com

# Setup automatic renewal
./scripts/ssl/generate-ssl.sh renewal
```

#### Option 2: Custom Certificates

```bash
# Place your certificates in docker/nginx/ssl/
cp your-certificate.pem docker/nginx/ssl/cert.pem
cp your-private-key.key docker/nginx/ssl/key.pem

# Set proper permissions
chmod 644 docker/nginx/ssl/cert.pem
chmod 600 docker/nginx/ssl/key.pem
```

### SSL Configuration

Update nginx configuration for your domain:

```nginx
# Edit docker/nginx/conf.d/default.conf
server_name yourdomain.com www.yourdomain.com;

# Update SSL certificate paths if needed
ssl_certificate /etc/nginx/ssl/cert.pem;
ssl_certificate_key /etc/nginx/ssl/key.pem;
```

## Monitoring & Health Checks

### Built-in Health Checks

All services include health checks:

```bash
# Check all services
./scripts/monitoring/health-check.sh

# Check specific components
./scripts/monitoring/health-check.sh docker
./scripts/monitoring/health-check.sh database
./scripts/monitoring/health-check.sh resources
```

### Health Check Endpoints

| Service | Health Check URL |
|---------|------------------|
| Backend | http://localhost:8080/api/actuator/health |
| User Frontend | http://localhost:3001/ |
| Admin Frontend | http://localhost:3000/ |
| Nginx | http://localhost:80/health |

### Monitoring Dashboard

Access built-in monitoring:

```bash
# View real-time logs
./start.sh logs -f

# Check resource usage
docker stats

# Monitor specific service
./start.sh logs backend -f
```

### Setting up Alerts

Configure webhooks in `.env`:

```bash
# Slack notifications
SLACK_WEBHOOK=https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK

# Generic webhook
WEBHOOK_URL=https://your-monitoring-service.com/webhook

# Enable email alerts
EMAIL_ALERTS=true
```

### Automated Monitoring

Setup cron job for regular health checks:

```bash
# Edit crontab
crontab -e

# Add health check every 5 minutes
*/5 * * * * /path/to/usdtClaudeCode/scripts/monitoring/health-check.sh >> /var/log/usdt-health.log 2>&1
```

## Logging

### Log Locations

```bash
logs/
├── backend/           # Application logs
│   ├── application.log
│   ├── audit.log
│   └── spring.log
├── mysql/            # Database logs
│   ├── error.log
│   └── slow.log
├── redis/            # Cache logs
│   └── redis.log
├── nginx/            # Web server logs
│   ├── access.log
│   └── error.log
├── admin/            # Admin frontend logs
└── user/             # User frontend logs
```

### Log Rotation

Automatic log rotation is configured:

```bash
# View log rotation configuration
cat docker/logging/logrotate.conf

# Manual log rotation
sudo logrotate -f docker/logging/logrotate.conf
```

### Log Levels

Configure logging levels in `.env`:

```bash
# Application log level
LOG_LEVEL=INFO  # TRACE, DEBUG, INFO, WARN, ERROR

# Enable audit logging
AUDIT_LOG_ENABLED=true

# Log format
LOG_FORMAT=json  # json or plain
```

### Centralized Logging

For production, consider centralized logging:

```bash
# Configure in docker-compose.yml
logging:
  driver: syslog
  options:
    syslog-address: "tcp://your-log-server:514"
    tag: "usdt-platform-{{.Name}}"
```

## Backup & Recovery

### Automated Backups

Backups are created automatically:

```bash
# Manual backup
./start.sh backup

# Check backup files
ls -la backups/
```

### Backup Contents

- **Database dump**: Complete MySQL database
- **Volume data**: Application data and files
- **Configuration files**: Environment and SSL certificates

### Restoration

```bash
# Stop services
./start.sh stop

# Restore from backup
mysql -u root -p < backups/db_YYYYMMDD_HHMMSS.sql

# Restore volume data
tar -xzf backups/volumes_YYYYMMDD_HHMMSS.tar.gz -C /path/to/restore/

# Start services
./start.sh start
```

### Backup Schedule

Configure automated backups:

```bash
# Add to crontab
0 2 * * * /path/to/usdtClaudeCode/start.sh backup >> /var/log/backup.log 2>&1
```

## Security

### Security Features

- **SSL/TLS encryption** for all communications
- **JWT authentication** with secure token handling
- **Rate limiting** on API endpoints
- **CORS protection** with allowlist
- **Security headers** (HSTS, CSP, etc.)
- **Input validation** and sanitization
- **SQL injection protection** via ORM
- **XSS protection** with content security policy

### Security Configuration

```bash
# Enable security features in .env
SECURITY_HEADERS_ENABLED=true
CONTENT_SECURITY_POLICY_ENABLED=true
RATE_LIMIT_ENABLED=true

# Configure CORS
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com

# Set strong passwords
PASSWORD_MIN_LENGTH=8
PASSWORD_REQUIRE_UPPERCASE=true
PASSWORD_REQUIRE_LOWERCASE=true
PASSWORD_REQUIRE_NUMBERS=true
PASSWORD_REQUIRE_SYMBOLS=true
```

### Security Best Practices

1. **Change default passwords** in `.env`
2. **Use strong JWT secrets** (256+ bits)
3. **Enable SSL certificates** for production
4. **Regular security updates** of Docker images
5. **Monitor logs** for suspicious activity
6. **Backup encryption** for sensitive data
7. **Network isolation** using Docker networks
8. **File permissions** (600 for keys, 644 for certificates)

## Troubleshooting

### Common Issues

#### 1. Services Won't Start

```bash
# Check Docker daemon
sudo systemctl status docker

# Check available disk space
df -h

# Check available memory
free -m

# View service logs
./start.sh logs [service-name]
```

#### 2. Database Connection Issues

```bash
# Check MySQL container
docker exec usdt-mysql mysql -u root -p -e "SELECT 1;"

# Check database configuration
docker exec usdt-mysql cat /etc/mysql/conf.d/my.cnf

# Reset database password
docker exec usdt-mysql mysql -u root -p -e "ALTER USER 'root'@'%' IDENTIFIED BY 'new-password';"
```

#### 3. SSL Certificate Problems

```bash
# Regenerate certificates
./scripts/ssl/generate-ssl.sh default

# Check certificate validity
./scripts/ssl/generate-ssl.sh validate

# Check nginx configuration
docker exec usdt-nginx nginx -t
```

#### 4. High Resource Usage

```bash
# Check resource consumption
docker stats

# Scale down resources in .env
BACKEND_MEMORY_LIMIT=1g
MYSQL_MEMORY_LIMIT=512m

# Restart services
./start.sh restart
```

#### 5. Port Conflicts

```bash
# Check port usage
netstat -tulpn | grep :80

# Change ports in .env
NGINX_HTTP_PORT=8080
NGINX_HTTPS_PORT=8443

# Update and restart
./start.sh restart
```

### Debug Mode

Enable debug mode for troubleshooting:

```bash
# Set in .env
DEBUG_ENABLED=true
LOG_LEVEL=DEBUG

# Restart services
./start.sh restart

# Monitor debug logs
./start.sh logs backend -f
```

### Getting Help

1. **Check logs first**: `./start.sh logs`
2. **Run health check**: `./scripts/monitoring/health-check.sh`
3. **Verify configuration**: Check `.env` file
4. **Check system resources**: `docker system df`
5. **Review documentation**: This file and inline comments

## Production Deployment

### Pre-deployment Checklist

- [ ] Update all passwords and secrets in `.env`
- [ ] Configure valid SSL certificates
- [ ] Set up monitoring and alerting
- [ ] Configure automated backups
- [ ] Review security settings
- [ ] Test disaster recovery procedures
- [ ] Set up log rotation
- [ ] Configure firewall rules
- [ ] Update DNS records
- [ ] Performance testing completed

### Production Environment Setup

```bash
# Set production environment
SPRING_PROFILES_ACTIVE=prod
NODE_ENV=production

# Disable debug features
DEBUG_ENABLED=false
DEV_MODE=false
CREATE_TEST_USERS=false

# Configure production URLs
APP_BASE_URL=https://yourdomain.com
```

### Performance Tuning

```bash
# Database optimization
MYSQL_MEMORY_LIMIT=2g
DB_MAX_CONNECTIONS=100

# Application optimization
BACKEND_MEMORY_LIMIT=3g
JAVA_OPTS="-Xms1g -Xmx3g -XX:+UseG1GC"

# Cache optimization
REDIS_MEMORY_LIMIT=1g
```

### Load Balancing

For high availability, consider:

```bash
# Multiple backend instances
docker-compose up --scale backend=3

# Load balancer configuration
# Update nginx upstream in docker/nginx/nginx.conf
```

## Maintenance

### Regular Maintenance Tasks

#### Daily

```bash
# Health check
./scripts/monitoring/health-check.sh

# Check disk space
df -h

# Review error logs
./start.sh logs | grep ERROR
```

#### Weekly

```bash
# Update Docker images
./start.sh update

# Clean up Docker system
./start.sh clean

# Verify backups
ls -la backups/
```

#### Monthly

```bash
# Update SSL certificates
./scripts/ssl/generate-ssl.sh renewal

# Security audit
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image usdt-backend

# Performance review
./scripts/monitoring/health-check.sh resources
```

### Scaling

#### Horizontal Scaling

```bash
# Scale specific services
docker-compose up --scale backend=3 --scale redis=2

# Update load balancer configuration
# Modify nginx upstream configuration
```

#### Vertical Scaling

```bash
# Increase resource limits in .env
BACKEND_MEMORY_LIMIT=4g
MYSQL_MEMORY_LIMIT=2g

# Apply changes
./start.sh restart
```

### Version Updates

```bash
# Backup current state
./start.sh backup

# Pull latest changes
git pull origin main

# Update dependencies
./start.sh build --no-cache

# Migrate database if needed
./start.sh migrate

# Restart services
./start.sh restart

# Verify deployment
./start.sh status
```

---

## Support

For additional support:

- **Documentation**: Check inline comments in configuration files
- **Logs**: Always check application logs first
- **Health Check**: Run comprehensive health check script
- **Community**: Check project GitHub issues
- **Commercial Support**: Contact development team

---

*Last updated: $(date '+%Y-%m-%d')*