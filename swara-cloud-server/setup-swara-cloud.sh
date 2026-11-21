#!/usr/bin/env bash
set -e
echo "=== Memasang Swara v4.8.8 Backend + Website & Admin Panel di Port 3600 ==="
SRC_DIR="$(cd "$(dirname "$0")" && pwd)"
sudo mkdir -p /opt/swara-cloud/web /opt/swara-cloud/assets /opt/swara-cloud/env /var/lib/swara-cloud
sudo cp -f "$SRC_DIR/server.py" /opt/swara-cloud/server.py
sudo cp -rf "$SRC_DIR/web/." /opt/swara-cloud/web/
sudo cp -rf "$SRC_DIR/assets/." /opt/swara-cloud/assets/
sudo cp -f "$SRC_DIR/env/.env.example" /opt/swara-cloud/env/.env.example
if [ -f "$SRC_DIR/setup-domain-swaraapp.sh" ]; then
  sudo cp -f "$SRC_DIR/setup-domain-swaraapp.sh" /opt/swara-cloud/setup-domain-swaraapp.sh
  sudo chmod +x /opt/swara-cloud/setup-domain-swaraapp.sh
fi
sudo chmod +x /opt/swara-cloud/server.py

# Buat file rahasia /opt/swara-cloud/env/.env secara lokal di VPS (tidak pernah di GitHub)
if [ ! -f /opt/swara-cloud/env/.env ]; then
  sudo cp /opt/swara-cloud/env/.env.example /opt/swara-cloud/env/.env
fi
sudo chmod 600 /opt/swara-cloud/env/.env

sudo tee /etc/systemd/system/swara-cloud.service >/dev/null <<'EOF'
[Unit]
Description=Swara Cloud Backend + Website & Admin Panel (Port 3600)
After=network.target

[Service]
Type=simple
WorkingDirectory=/opt/swara-cloud
EnvironmentFile=/opt/swara-cloud/env/.env
ExecStart=/usr/bin/python3 /opt/swara-cloud/server.py
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable swara-cloud.service
sudo systemctl restart swara-cloud.service
sleep 2
curl -s http://127.0.0.1:3600/health && echo ""
echo "=== Swara v4.8.8 AKTIF! File rahasia aman di /opt/swara-cloud/env/.env (chmod 600) ==="
