#!/bin/bash
# VPS First-Time Setup Script
# Run on a fresh Azure Ubuntu 22.04 VM (B2s minimum — 2 vCPU, 4GB RAM)
# Usage: bash scripts/setup_vps.sh your-domain.com

set -e

DOMAIN=${1:-"your-domain.com"}
echo "================================================"
echo "  Quran App — VPS Setup"
echo "  Domain: $DOMAIN"
echo "================================================"

# ── 1. System update ─────────────────────────────────────────────────────────
echo "[1/7] Updating system packages..."
sudo apt-get update -q && sudo apt-get upgrade -y -q

# ── 2. Install Docker ─────────────────────────────────────────────────────────
echo "[2/7] Installing Docker..."
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"
sudo systemctl enable docker

# ── 3. Install Docker Compose plugin ─────────────────────────────────────────
echo "[3/7] Installing Docker Compose..."
sudo apt-get install -y docker-compose-plugin

# ── 4. Install Certbot for SSL ────────────────────────────────────────────────
echo "[4/7] Installing Certbot..."
sudo apt-get install -y certbot

# ── 5. Create app directory ───────────────────────────────────────────────────
echo "[5/7] Creating app directory..."
sudo mkdir -p /opt/quran-app
sudo chown "$USER":"$USER" /opt/quran-app

# ── 6. Firewall rules ─────────────────────────────────────────────────────────
echo "[6/7] Configuring firewall..."
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw --force enable

# ── 7. Instructions for next steps ───────────────────────────────────────────
echo ""
echo "[7/7] Setup complete!"
echo ""
echo "Next steps:"
echo "  1. Log out and back in for Docker group to take effect"
echo "  2. Clone your repo:  git clone <repo-url> /opt/quran-app"
echo "  3. Configure env:    cp /opt/quran-app/.env.example /opt/quran-app/.env"
echo "                       nano /opt/quran-app/.env"
echo "  4. Get SSL cert:     sudo certbot certonly --standalone -d $DOMAIN"
echo "  5. Copy certs:       sudo cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem /opt/quran-app/nginx/ssl/"
echo "                       sudo cp /etc/letsencrypt/live/$DOMAIN/privkey.pem /opt/quran-app/nginx/ssl/"
echo "  6. Start services:   cd /opt/quran-app && docker compose up -d"
echo "  7. Run ingestion:    docker compose exec fastapi python ingestion/ingest_ayahs.py"
echo "                       docker compose exec fastapi python ingestion/ingest_tafsir.py"
echo "                       docker compose exec fastapi python ingestion/ingest_hadith.py"
echo "  8. Verify:           curl https://$DOMAIN/api/health"
echo ""
echo "Add SSL renewal cron (run: crontab -e):"
echo "  0 3 * * * certbot renew --pre-hook 'docker stop quran_nginx' --post-hook 'docker start quran_nginx' --quiet"
