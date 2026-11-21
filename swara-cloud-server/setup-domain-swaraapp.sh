#!/usr/bin/env bash
set -e
DOMAIN="swaraapp.my.id"
echo "=== Menyiapkan Nginx Reverse Proxy & SSL HTTPS untuk $DOMAIN -> 127.0.0.1:3600 ==="
sudo apt-get update -y
sudo apt-get install -y nginx certbot python3-certbot-nginx

sudo tee /etc/nginx/sites-available/swaraapp.my.id >/dev/null <<'EOF'
server {
    listen 80;
    server_name swaraapp.my.id www.swaraapp.my.id;

    client_max_body_size 100M;

    location / {
        proxy_pass http://127.0.0.1:3600;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

sudo ln -sf /etc/nginx/sites-available/swaraapp.my.id /etc/nginx/sites-enabled/swaraapp.my.id
sudo nginx -t
sudo systemctl reload nginx
sudo certbot --nginx -d swaraapp.my.id -d www.swaraapp.my.id --non-interactive --agree-tos -m vipercell.id@gmail.com --redirect || echo "Catatan: Pastikan DNS A Record swaraapp.my.id sudah mengarah ke 103.119.237.122 lalu jalankan ulang skrip ini untuk SSL."
echo "=== Selesai! Cek https://swaraapp.my.id dan https://swaraapp.my.id/admin ==="
