# Migration Guide: AWS EC2 to DigitalOcean VPS

This guide walks you through migrating the Luna backend from AWS EC2 to DigitalOcean VPS with Docker Compose (v2), Nginx, and Certbot for HTTPS.

**Deployment Strategy:** This guide uses a registry-based deployment where pre-built Docker images are pulled from a container registry (Docker Hub, GitHub Container Registry, etc.). You only need `docker-compose.yml` and `.env` files on the server - no source code required.

## Quick Reference

Once setup is complete, your typical deployment workflow will be:

```bash
cd ~/luna-backend
docker compose pull    # Pull latest images
docker compose up -d   # Start/restart services
docker compose logs -f # View logs
```

## Prerequisites

- DigitalOcean account
- Domain name configured
- Access to current EC2 instance
- Docker images pushed to a registry (Docker Hub, GHCR, or private registry)
- Your `docker-compose.yml` and `.env` files from EC2
- Basic knowledge of Linux/SSH

## 1. Create DigitalOcean Droplet

1. Log in to DigitalOcean dashboard
2. Create a new Droplet:
   - **Distribution:** Ubuntu 22.04 LTS
   - **Plan:** Basic (at least $12/month for 2GB RAM)
   - **Datacenter:** Choose closest to your users
   - **Authentication:** SSH keys (recommended) or password
   - **Optional:** Enable backups

3. Note your droplet's IP address

## 2. Initial Server Setup

### Connect to your droplet

```bash
ssh root@your-droplet-ip
```

### Update system packages

```bash
apt update && apt upgrade -y
```

### Create a non-root user (recommended)

```bash
# Create user
adduser deployuser

# Add to sudo group
usermod -aG sudo deployuser

# Switch to new user
su - deployuser
```

## 3. Install Required Software

### Install Docker Engine

```bash
# Download Docker installation script
curl -fsSL https://get.docker.com -o get-docker.sh

# Run installation
sudo sh get-docker.sh

# Add user to docker group
sudo usermod -aG docker $USER

# Apply group changes (or logout/login)
newgrp docker

# Verify installation
docker --version
```

### Install Docker Compose Plugin (v2)

```bash
# Install compose plugin
sudo apt install docker-compose-plugin -y

# Verify installation (note: 'docker compose' not 'docker-compose')
docker compose version
```

### Install Nginx

```bash
sudo apt install nginx -y

# Enable and start Nginx
sudo systemctl enable nginx
sudo systemctl start nginx

# Check status
sudo systemctl status nginx
```

### Install Certbot for SSL

```bash
sudo apt install certbot python3-certbot-nginx -y

# Verify installation
certbot --version
```

## 4. Transfer Configuration Files

You only need `docker-compose.yml` and `.env` files on the server (images will be pulled from registry).

### Create application directory

```bash
mkdir -p ~/luna-backend
cd ~/luna-backend
```

### Transfer files from EC2

**Option A: Copy from EC2 directly**

```bash
# From your local machine
scp ec2-user@your-ec2-ip:/path/to/luna-backend/docker-compose.yml ~/
scp ec2-user@your-ec2-ip:/path/to/luna-backend/.env ~/

# Upload to DigitalOcean
scp docker-compose.yml deployuser@your-droplet-ip:~/luna-backend/
scp .env deployuser@your-droplet-ip:~/luna-backend/
```

**Option B: Create files manually**

If you don't have direct access, create them on DigitalOcean:

```bash
cd ~/luna-backend
nano docker-compose.yml
# Paste your docker-compose.yml content

nano .env
# Paste your .env content
```

## 5. Verify Docker Compose Configuration

Your `docker-compose.yml` should reference pre-built images from a registry:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: luna-postgres
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    restart: unless-stopped

  luna-app:
    image: your-registry/luna-backend:latest  # Your pre-built image
    container_name: luna-app
    env_file:
      - .env
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    restart: unless-stopped

volumes:
  postgres_data:
```

**Note:** Make sure your `luna-app` image is pushed to a registry (Docker Hub, GitHub Container Registry, or private registry).

## 6. Configure Environment Variables

```bash
cd ~/luna-backend

# Edit your .env file
nano .env
```

**Important variables to set:**

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/luna_db
DB_USERNAME=postgres
DB_PASSWORD=your-secure-password

# JWT
JWT_SECRET=your-256-bit-secret-key
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Email (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Cloudinary
CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name

# Service API Key
SERVICE_API_KEY=your-service-api-key

# Optional settings
DEVICE_VERIFICATION_ENABLED=true
SCHEDULING_ENABLED=true
POST_CLEANUP_CRON=0 2 * * *
```

## 7. Database Migration

### Option A: Migrate Existing PostgreSQL Data

**On EC2 instance, create backup:**

```bash
# Using docker compose
docker compose exec postgres pg_dump -U postgres luna_db > luna_backup.sql

# Or if postgres is installed directly
pg_dump -h localhost -U postgres luna_db > luna_backup.sql
```

**Transfer backup to DigitalOcean:**

```bash
scp luna_backup.sql deployuser@your-droplet-ip:~/luna-backend/
```

**On DigitalOcean, restore database:**

```bash
cd ~/luna-backend

# Start only postgres service
docker compose up -d postgres

# Wait a few seconds for postgres to initialize
sleep 10

# Create database
docker compose exec postgres psql -U postgres -c "CREATE DATABASE luna_db;"

# Restore data
docker compose exec -T postgres psql -U postgres luna_db < luna_backup.sql

# Verify restoration
docker compose exec postgres psql -U postgres -d luna_db -c "\dt"
```

### Option B: Fresh Database

If starting fresh or if Flyway migrations will handle schema:

```bash
cd ~/luna-backend

# Start all services (Flyway will run migrations automatically)
docker compose up -d
```

## 8. Configure Nginx as Reverse Proxy

### Create Nginx configuration

```bash
sudo nano /etc/nginx/sites-available/luna-backend
```

**Add this configuration:**

```nginx
upstream luna_backend {
    server localhost:8080;
}

server {
    listen 80;
    listen [::]:80;
    server_name yourdomain.com www.yourdomain.com;

    # Allow larger request body for image uploads
    client_max_body_size 10M;

    location / {
        proxy_pass http://luna_backend;
        proxy_http_version 1.1;

        # WebSocket support (if needed)
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';

        # Standard proxy headers
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeout settings
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        proxy_cache_bypass $http_upgrade;
    }

    # Health check endpoint
    location /health {
        proxy_pass http://luna_backend/health;
        access_log off;
    }
}
```

### Enable the site

```bash
# Create symbolic link
sudo ln -s /etc/nginx/sites-available/luna-backend /etc/nginx/sites-enabled/

# Remove default site (optional)
sudo rm /etc/nginx/sites-enabled/default

# Test configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

## 9. Update DNS Records

**Before setting up SSL, update your DNS:**

1. Go to your domain registrar's DNS management
2. Update A records:
   - `@` (root) → `your-droplet-ip`
   - `www` → `your-droplet-ip`

3. Wait for DNS propagation (can take 5-60 minutes)

**Verify DNS propagation:**

```bash
# Check if DNS is updated
dig yourdomain.com +short
nslookup yourdomain.com
```

## 10. Setup SSL Certificate with Certbot

### Obtain SSL certificate

```bash
# Run Certbot (replace with your domain)
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# Follow prompts:
# - Enter email address
# - Agree to terms
# - Choose whether to redirect HTTP to HTTPS (recommended: yes)
```

### Test automatic renewal

```bash
sudo certbot renew --dry-run
```

### Check certificate status

```bash
sudo certbot certificates
```

## 11. Start Application

```bash
cd ~/luna-backend

# Pull latest images from registry
docker compose pull

# Start all services in detached mode
docker compose up -d

# View logs
docker compose logs -f

# Check running containers
docker compose ps
```

### Verify services are running

```bash
# Check application health
curl http://localhost:8080/health

# Check via domain
curl https://yourdomain.com/health

# Check Swagger UI (if enabled)
curl https://yourdomain.com/swagger-ui.html
```

## 12. Configure Firewall (UFW)

```bash
# Check UFW status
sudo ufw status

# Allow SSH (IMPORTANT: do this first!)
sudo ufw allow OpenSSH

# Allow Nginx (HTTP + HTTPS)
sudo ufw allow 'Nginx Full'

# Enable firewall
sudo ufw enable

# Verify rules
sudo ufw status verbose
```

## 13. Post-Migration Verification

### Health Checks

```bash
# Application health
curl https://yourdomain.com/health

# API endpoints
curl -X POST https://yourdomain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# Check database connection
docker compose exec postgres psql -U postgres -d luna_db -c "SELECT COUNT(*) FROM users;"
```

### Check logs

```bash
# Application logs
docker compose logs luna-app

# Postgres logs
docker compose logs postgres

# Nginx logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

### Monitor resources

```bash
# Check disk space
df -h

# Check memory usage
free -h

# Check Docker resource usage
docker stats
```

## 14. Maintenance Commands

### Updating the application

```bash
cd ~/luna-backend

# Pull latest images from registry
docker compose pull

# Restart with new images
docker compose up -d

# View logs
docker compose logs -f
```

### Full restart (if needed)

```bash
cd ~/luna-backend

# Stop all services
docker compose down

# Pull latest images
docker compose pull

# Start services
docker compose up -d
```

### Database backup

```bash
# Create backup
docker compose exec postgres pg_dump -U postgres luna_db > backup_$(date +%Y%m%d_%H%M%S).sql

# Or create automated backup script
nano ~/backup.sh
```

**Backup script (`backup.sh`):**

```bash
#!/bin/bash
BACKUP_DIR=~/backups
mkdir -p $BACKUP_DIR
cd ~/luna-backend
docker compose exec -T postgres pg_dump -U postgres luna_db > $BACKUP_DIR/luna_$(date +%Y%m%d_%H%M%S).sql

# Keep only last 7 days of backups
find $BACKUP_DIR -name "luna_*.sql" -mtime +7 -delete
```

```bash
chmod +x ~/backup.sh

# Add to crontab for daily backups at 3 AM
crontab -e
# Add: 0 3 * * * /home/deployuser/backup.sh
```

### View container logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f luna-app

# Last 100 lines
docker compose logs --tail=100 luna-app
```

### Restart services

```bash
# Restart all
docker compose restart

# Restart specific service
docker compose restart luna-app

# Stop all
docker compose down

# Start all
docker compose up -d
```

## 15. Troubleshooting

### Application won't start

```bash
# Check logs
docker compose logs luna-app

# Check if port 8080 is in use
sudo netstat -tulpn | grep 8080

# Verify environment variables
docker compose config
```

### Database connection issues

```bash
# Check if postgres is running
docker compose ps postgres

# Check postgres logs
docker compose logs postgres

# Test connection
docker compose exec postgres psql -U postgres -d luna_db -c "SELECT 1;"
```

### Nginx errors

```bash
# Check configuration
sudo nginx -t

# Check logs
sudo tail -f /var/log/nginx/error.log

# Restart Nginx
sudo systemctl restart nginx
```

### SSL certificate issues

```bash
# Check certificate status
sudo certbot certificates

# Renew certificate
sudo certbot renew --force-renewal

# Check Nginx SSL configuration
sudo cat /etc/nginx/sites-available/luna-backend
```

### Out of disk space

```bash
# Check disk usage
df -h

# Remove unused Docker resources
docker system prune -a

# Remove old logs
sudo journalctl --vacuum-time=3d
```

## 16. Performance Optimization (Optional)

### Enable Nginx caching

Add to your Nginx config:

```nginx
# Add at top of file
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m max_size=1g inactive=60m;

# In location block
location /api/ {
    proxy_cache api_cache;
    proxy_cache_valid 200 5m;
    proxy_cache_bypass $http_cache_control;
    add_header X-Cache-Status $upstream_cache_status;

    # ... rest of proxy settings
}
```

### Enable Gzip compression

Add to Nginx config:

```nginx
gzip on;
gzip_vary on;
gzip_min_length 1000;
gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
```

### Adjust Docker Compose resources

If needed, limit resources in `docker-compose.yml`:

```yaml
services:
  luna-app:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          memory: 512M
```

## 17. Monitoring and Alerts (Optional)

### Setup DigitalOcean monitoring

1. Enable monitoring in droplet settings
2. Configure alerts for:
   - CPU usage > 80%
   - Memory usage > 85%
   - Disk usage > 90%

### Install monitoring tools

```bash
# Install htop for resource monitoring
sudo apt install htop -y

# Install ctop for Docker container monitoring
sudo wget https://github.com/bcicen/ctop/releases/download/v0.7.7/ctop-0.7.7-linux-amd64 -O /usr/local/bin/ctop
sudo chmod +x /usr/local/bin/ctop
```

## Summary

You've successfully migrated Luna backend from AWS EC2 to DigitalOcean! Your setup now includes:

- ✅ Ubuntu 22.04 LTS server
- ✅ Docker Compose v2 for container orchestration
- ✅ Nginx as reverse proxy
- ✅ SSL/HTTPS with Certbot (auto-renewal)
- ✅ PostgreSQL database
- ✅ Firewall configured with UFW
- ✅ Production-ready environment

## Next Steps

1. Update your frontend to use the new domain
2. Setup automated backups
3. Configure monitoring and alerts
4. Test all API endpoints thoroughly
5. Decommission AWS EC2 instance (after confirming everything works)

## Support

For issues specific to:
- **Luna Backend:** Check `/docs` folder in the repository
- **DigitalOcean:** https://docs.digitalocean.com
- **Docker:** https://docs.docker.com
- **Nginx:** https://nginx.org/en/docs/
