#!/bin/bash

# Wait for MySQL script
# This script waits for MySQL to be ready before starting the Spring Boot application

set -e

host="$1"
port="${2:-3306}"
username="${3:-root}"
password="$4"

echo "Waiting for MySQL at $host:$port to be ready..."

until mysql -h"$host" -P"$port" -u"$username" -p"$password" -e "SELECT 1" >/dev/null 2>&1; do
  echo "MySQL is unavailable - sleeping for 2 seconds"
  sleep 2
done

echo "MySQL is ready!"

# Test specific database exists
database="${5:-usdt_trading_platform}"
echo "Testing database $database..."

until mysql -h"$host" -P"$port" -u"$username" -p"$password" -D"$database" -e "SELECT 1" >/dev/null 2>&1; do
  echo "Database $database is not ready - sleeping for 2 seconds"
  sleep 2
done

echo "Database $database is ready!"
echo "Starting Spring Boot application..."

# Execute the original command
exec "$@"