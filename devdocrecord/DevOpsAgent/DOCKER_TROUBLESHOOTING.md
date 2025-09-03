# Docker Deployment Troubleshooting Guide

## Quick Diagnosis Commands

```bash
# System overview
./start.sh info

# Service status
./start.sh status

# Health check
./scripts/monitoring/health-check.sh

# Resource usage
docker stats --no-stream

# Recent logs
./start.sh logs --tail=50
```

## Common Issues & Solutions

### 1. Container Startup Issues

#### Symptom: Services fail to start or exit immediately

**Diagnosis:**
```bash
# Check container exit codes
docker-compose ps

# View startup logs
./start.sh logs [service-name]

# Check system resources
free -m && df -h
```

**Common Causes & Solutions:**

| Issue | Solution |
|-------|----------|
| Out of memory | Increase system memory or reduce container limits |
| Port conflicts | Change ports in `.env` file |
| Permission issues | `sudo chown -R $USER:$USER .` |
| Missing dependencies | Run `./start.sh build --no-cache` |

#### Specific Error Messages:

**"Port already in use"**
```bash
# Find process using port
netstat -tulpn | grep :8080

# Change port in .env
BACKEND_PORT=8081

# Restart
./start.sh restart
```

**"No space left on device"**
```bash
# Clean Docker system
docker system prune -af

# Clean old images
docker image prune -af

# Check disk usage
docker system df
```

### 2. Database Connection Issues

#### Symptom: Backend can't connect to MySQL

**Diagnosis:**
```bash
# Test MySQL connectivity
docker exec usdt-mysql mysql -u root -p -e "SELECT 1;"

# Check MySQL logs
./start.sh logs mysql

# Verify network connectivity
docker exec usdt-backend nc -zv mysql 3306
```

**Solutions:**

```bash
# Reset MySQL root password
docker exec usdt-mysql mysql -u root -p -e "ALTER USER 'root'@'%' IDENTIFIED BY 'new-password';"

# Recreate MySQL container
docker-compose rm -f mysql
docker volume rm usdt-trading-platform_mysql_data
./start.sh start
```

### 3. SSL/HTTPS Issues

#### Symptom: SSL certificate errors or HTTPS not working

**Diagnosis:**
```bash
# Check certificate validity
./scripts/ssl/generate-ssl.sh validate

# Test HTTPS endpoint
curl -k https://localhost/health

# Check nginx configuration
docker exec usdt-nginx nginx -t
```

**Solutions:**

```bash
# Regenerate certificates
./scripts/ssl/generate-ssl.sh default

# Check certificate permissions
ls -la docker/nginx/ssl/

# Reload nginx
docker exec usdt-nginx nginx -s reload
```

### 4. High CPU/Memory Usage

#### Symptom: System slow or containers being killed (OOMKilled)

**Diagnosis:**
```bash
# Monitor resource usage
docker stats

# Check container limits
docker inspect usdt-backend | grep -A 10 "Memory"

# System resource usage
htop
```

**Solutions:**

```bash
# Reduce memory limits in .env
BACKEND_MEMORY_LIMIT=1g
MYSQL_MEMORY_LIMIT=512m

# Optimize JVM settings
JAVA_OPTS="-Xms256m -Xmx1g -XX:+UseG1GC"

# Scale down services
docker-compose up --scale backend=1
```

### 5. Network Connectivity Issues

#### Symptom: Services can't communicate with each other

**Diagnosis:**
```bash
# Check Docker networks
docker network ls

# Inspect network configuration
docker network inspect usdt-trading-platform_network

# Test connectivity between containers
docker exec usdt-backend ping mysql
```

**Solutions:**

```bash
# Recreate Docker network
docker-compose down
docker network prune
./start.sh start

# Check firewall rules
sudo ufw status

# Verify DNS resolution in containers
docker exec usdt-backend nslookup mysql
```

### 6. Frontend Build Issues

#### Symptom: Frontend containers fail to build or serve files

**Diagnosis:**
```bash
# Check build logs
docker-compose build user-frontend --no-cache

# Verify built files
docker run --rm usdt-user-frontend ls -la /usr/share/nginx/html/

# Test nginx configuration
docker exec usdt-nginx nginx -t
```

**Solutions:**

```bash
# Clean build cache and rebuild
docker-compose build --no-cache frontend
docker system prune -f

# Check Node.js version compatibility
# Verify package.json scripts

# Test frontend individually
cd frontend/user
npm install
npm run build
```

### 7. Persistent Volume Issues

#### Symptom: Data not persisting or volume mount errors

**Diagnosis:**
```bash
# List Docker volumes
docker volume ls | grep usdt

# Inspect volume
docker volume inspect usdt-trading-platform_mysql_data

# Check mount permissions
docker exec usdt-mysql ls -la /var/lib/mysql
```

**Solutions:**

```bash
# Fix volume permissions
sudo chown -R 999:999 data/mysql/  # MySQL UID:GID

# Recreate volumes
docker-compose down -v
docker volume prune
./start.sh start

# Backup and restore data
./start.sh backup
```

## Advanced Debugging

### 1. Container Shell Access

```bash
# Access backend container
docker exec -it usdt-backend bash

# Access MySQL container
docker exec -it usdt-mysql mysql -u root -p

# Access nginx container
docker exec -it usdt-nginx sh
```

### 2. Debug Mode

Enable debug logging:

```bash
# Set in .env
DEBUG_ENABLED=true
LOG_LEVEL=DEBUG
SPRING_LOGGING_LEVEL_ROOT=DEBUG

# Restart services
./start.sh restart

# Monitor debug logs
./start.sh logs backend -f | grep DEBUG
```

### 3. Network Debugging

```bash
# Create debug container on same network
docker run -it --network usdt-trading-platform_network --rm alpine sh

# Inside container, test connectivity
nc -zv mysql 3306
nc -zv redis 6379
nc -zv backend 8080

# Test DNS resolution
nslookup mysql
nslookup backend
```

### 4. Performance Analysis

```bash
# Container resource usage
docker stats --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"

# Application performance
curl -w "@curl-format.txt" -o /dev/null -s "http://localhost:8080/api/actuator/health"

# Database performance
docker exec usdt-mysql mysql -u root -p -e "SHOW PROCESSLIST;"
```

### 5. Log Analysis

```bash
# Search for specific errors
./start.sh logs | grep -i "error\|exception\|failed"

# Monitor real-time logs with filters
./start.sh logs -f | grep -E "(ERROR|WARN|Exception)"

# Export logs for analysis
./start.sh logs > system-logs-$(date +%Y%m%d).log
```

## Service-Specific Troubleshooting

### MySQL

```bash
# Check MySQL variables
docker exec usdt-mysql mysql -u root -p -e "SHOW VARIABLES LIKE 'max_connections';"

# Check MySQL status
docker exec usdt-mysql mysql -u root -p -e "SHOW STATUS LIKE 'Threads_connected';"

# Optimize MySQL
docker exec usdt-mysql mysql -u root -p -e "OPTIMIZE TABLE user_table;"

# Reset MySQL if corrupted
docker-compose rm -f mysql
docker volume rm usdt-trading-platform_mysql_data
./start.sh start
```

### Redis

```bash
# Check Redis info
docker exec usdt-redis redis-cli info memory

# Monitor Redis commands
docker exec usdt-redis redis-cli monitor

# Clear Redis cache
docker exec usdt-redis redis-cli flushall

# Check Redis configuration
docker exec usdt-redis cat /etc/redis/redis.conf
```

### Backend (Spring Boot)

```bash
# Check application properties
docker exec usdt-backend cat /app/application.yml

# View JVM information
docker exec usdt-backend java -XX:+PrintFlagsFinal -version

# Application endpoints
curl http://localhost:8080/api/actuator/info
curl http://localhost:8080/api/actuator/metrics
curl http://localhost:8080/api/actuator/env
```

### Nginx

```bash
# Test nginx configuration
docker exec usdt-nginx nginx -t

# Reload nginx
docker exec usdt-nginx nginx -s reload

# Check nginx processes
docker exec usdt-nginx ps aux | grep nginx

# Test upstream connectivity
docker exec usdt-nginx nc -zv backend 8080
```

## Recovery Procedures

### 1. Complete System Recovery

```bash
# Stop all services
./start.sh stop

# Clean Docker system
docker system prune -af
docker volume prune -f

# Rebuild from scratch
./start.sh build --no-cache

# Start services
./start.sh start

# Verify health
./scripts/monitoring/health-check.sh
```

### 2. Database Recovery

```bash
# Stop backend to prevent connections
docker-compose stop backend

# Create database backup
docker exec usdt-mysql mysqldump -u root -p --all-databases > backup.sql

# Reset database
docker-compose rm -f mysql
docker volume rm usdt-trading-platform_mysql_data

# Start MySQL
docker-compose up -d mysql

# Wait for MySQL to be ready
sleep 30

# Restore database
docker exec -i usdt-mysql mysql -u root -p < backup.sql

# Start backend
docker-compose up -d backend
```

### 3. Configuration Reset

```bash
# Backup current configuration
cp .env .env.backup

# Reset to defaults
cp .env.example .env

# Edit configuration
nano .env

# Apply changes
./start.sh restart
```

## Prevention Best Practices

### 1. Monitoring Setup

```bash
# Setup cron job for health checks
crontab -e
# Add: */5 * * * * /path/to/scripts/monitoring/health-check.sh

# Setup log rotation
sudo cp docker/logging/logrotate.conf /etc/logrotate.d/usdt-platform

# Monitor disk space
echo 'df -h | grep -E "(Filesystem|/dev/)" | mail -s "Disk Usage Report" admin@yourdomain.com' | crontab -e
```

### 2. Backup Strategy

```bash
# Automated backups
echo '0 2 * * * /path/to/start.sh backup' | crontab -e

# Test backup restoration monthly
./start.sh backup
# Restore in test environment and verify
```

### 3. Update Procedures

```bash
# Before updates
./start.sh backup

# Update with verification
git pull origin main
./start.sh build --no-cache
./start.sh restart
./scripts/monitoring/health-check.sh

# Rollback if issues
git checkout previous-stable-version
./start.sh restart
```

### 4. Security Monitoring

```bash
# Monitor failed login attempts
./start.sh logs backend | grep "authentication failed"

# Check for suspicious activity
./start.sh logs nginx | grep -E "4[0-9][0-9]|5[0-9][0-9]"

# SSL certificate expiration monitoring
./scripts/ssl/generate-ssl.sh validate
```

## Emergency Contacts & Escalation

1. **Check system status**: Run health check script
2. **Review recent changes**: Check git log and deployment history
3. **Gather information**: Collect logs and system information
4. **Try quick fixes**: Restart services, check resources
5. **Escalate if needed**: Contact development team with:
   - Error messages
   - System information
   - Steps already attempted
   - Impact on users

## Useful Commands Reference

```bash
# System information
./start.sh info
docker system df
docker system events --since '1h' --until '0m'

# Service management
./start.sh start|stop|restart|status
./start.sh logs [service] [-f]
./start.sh build [--no-cache]

# Health and monitoring
./scripts/monitoring/health-check.sh [component]
docker stats --no-stream
docker exec [container] [command]

# Maintenance
./start.sh backup
./start.sh clean
./start.sh update

# SSL management
./scripts/ssl/generate-ssl.sh [command]

# Network diagnostics
docker network ls
docker network inspect [network]
netstat -tulpn | grep [port]
```

Remember: Always backup before making changes, and test solutions in a non-production environment first.