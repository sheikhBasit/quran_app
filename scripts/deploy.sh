#!/bin/bash
# Deploy script — run on VPS to pull latest code and restart services
# Usage: bash scripts/deploy.sh

set -e

APP_DIR="/opt/quran-app"
echo "================================================"
echo "  Quran App — Deploy Update"
echo "================================================"

cd "$APP_DIR"

echo "[1/4] Pulling latest code..."
git pull origin main

echo "[2/4] Rebuilding FastAPI container..."
docker compose up -d --build fastapi

echo "[3/4] Waiting for FastAPI to be healthy..."
sleep 5

echo "[4/4] Health check..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/chat/health)
if [ "$STATUS" = "200" ]; then
    echo "✅ Deploy successful — /api/health returned 200"
else
    echo "❌ Health check failed — status $STATUS"
    echo "   Check logs: docker compose logs -f fastapi"
    exit 1
fi
