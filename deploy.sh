#!/bin/bash

# Luna Deployment Script for EC2

set -e

echo "🚀 Starting Luna deployment..."

# Load environment variables
if [ -f .env.production ]; then
    export $(cat .env.production | grep -v '^#' | xargs)
    echo "✅ Environment variables loaded"
else
    echo "❌ .env.production not found!"
    exit 1
fi

# Stop existing containers
echo "🛑 Stopping existing containers..."
docker-compose down || true

# Pull latest code (if using git)
# git pull origin main

# Build and start containers
echo "🔨 Building Docker image..."
docker-compose build --no-cache

echo "🚀 Starting containers..."
docker-compose up -d

# Wait for health check
echo "⏳ Waiting for application to be healthy..."
sleep 10

# Check health
if curl -f http://localhost:8080/health > /dev/null 2>&1; then
    echo "✅ Deployment successful! Application is healthy."
else
    echo "❌ Health check failed!"
    docker-compose logs --tail=50
    exit 1
fi

echo "🎉 Deployment complete!"
