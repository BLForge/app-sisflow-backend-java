#!/usr/bin/env bash
# =============================================================================
# install.sh — sisflow full setup (Docker already installed)
# Ubuntu 22.04 LTS — run with sudo
# =============================================================================
set -euo pipefail

APP_DIR="/opt/sisflow"
ENV_FILE="$APP_DIR/.env"

# -----------------------------------------------------------------------------
echo "==> [0/6] Cleanup — removing any previous failed install"

# Stop and remove existing containers
docker compose -f "$APP_DIR/docker-compose.yml" down 2>/dev/null || true

# Remove old app directory
if [ -d "$APP_DIR" ]; then
  echo "    Removing old $APP_DIR..."
  rm -rf "$APP_DIR"
fi

# Remove old Docker images
docker rmi sisflow:latest 2>/dev/null && echo "    Removed old sisflow:latest image." || echo "    No old image found, skipping."

# Remove dangling images
docker image prune -f 2>/dev/null || true

# -----------------------------------------------------------------------------
echo "==> [1/6] System update"
apt-get update -y

# -----------------------------------------------------------------------------
echo "==> [2/6] Install dependencies (git, Java 17 Temurin)"
apt-get install -y wget apt-transport-https git

wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg

echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] \
https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" \
  > /etc/apt/sources.list.d/adoptium.list

apt-get update -y
apt-get install -y temurin-17-jdk

echo "    Java: $(java -version 2>&1 | head -1)"

# -----------------------------------------------------------------------------
echo "==> [3/6] Clone private GitHub repository (snortexware/sisflow-backend)"

GH_REPO="snortexware/sisflow-backend"

read -rp "  GitHub username: " GH_USER
read -rp "  GitHub Personal Access Token (PAT): " GH_TOKEN
echo ""

REPO_URL="https://${GH_USER}:${GH_TOKEN}@github.com/${GH_REPO}.git"

git clone "$REPO_URL" "$APP_DIR"
echo "    Cloned into $APP_DIR"

# Remove token from git remote URL after clone so it's not stored in plain text
git -C "$APP_DIR" remote set-url origin "https://github.com/${GH_REPO}.git"

# Fix mvnw permissions so Docker can execute it during build
sudo chmod +x "$APP_DIR/mvnw"
sudo git config --global --add safe.directory "$APP_DIR"
sudo git -C "$APP_DIR" update-index --chmod=+x mvnw
echo "    Fixed mvnw permissions"

# -----------------------------------------------------------------------------
echo "==> [4/6] Create shared Traefik network (if not exists)"
docker network create traefik-public 2>/dev/null || echo "    Network already exists, skipping."

# -----------------------------------------------------------------------------
echo "==> [5/6] Firewall — open ports 80 and 443"
iptables -I INPUT -p tcp --dport 80  -j ACCEPT
iptables -I INPUT -p tcp --dport 443 -j ACCEPT
apt-get install -y iptables-persistent
netfilter-persistent save

# -----------------------------------------------------------------------------
echo ""
echo "==> [6/6] Configure environment variables"

echo "    Enter your environment values (press Enter to accept the default shown in brackets):"
echo ""

read -rp "  SPRING_DATASOURCE_URL [jdbc:postgresql://localhost:5432/postgres]: " DS_URL
DS_URL="${DS_URL:-jdbc:postgresql://localhost:5432/postgres}"

read -rp "  SPRING_DATASOURCE_USERNAME [postgres]: " DS_USER
DS_USER="${DS_USER:-postgres}"

read -rp "  SPRING_DATASOURCE_PASSWORD: " DS_PASS

read -rp "  JWT_SECRET: " JWT_SECRET
echo ""

cat > "$ENV_FILE" <<EOF
SPRING_DATASOURCE_URL=${DS_URL}
SPRING_DATASOURCE_USERNAME=${DS_USER}
SPRING_DATASOURCE_PASSWORD=${DS_PASS}
JWT_SECRET=${JWT_SECRET}
EOF

chmod 600 "$ENV_FILE"
echo "    .env written to $ENV_FILE"

# -----------------------------------------------------------------------------
echo ""
echo "==> Starting app with docker compose..."
docker builder prune -f
docker compose -f "$APP_DIR/docker-compose.yml" --env-file "$ENV_FILE" up -d --build --no-deps

echo ""
echo "  Done! sisflow is running."
echo "  Check logs with: docker compose -f $APP_DIR/docker-compose.yml logs -f"
echo ""
