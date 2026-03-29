#!/usr/bin/env bash
set -euo pipefail

# ── MonteWeb Deployment Script ─────────────────────────────────────────────
# Usage:
#   ./scripts/deploy.sh                  # Standard deploy (build + restart)
#   ./scripts/deploy.sh --new-tunnel     # Deploy + create new Cloudflare tunnel
#   ./scripts/deploy.sh --backend-only   # Rebuild only backend
#   ./scripts/deploy.sh --frontend-only  # Rebuild only frontend
#   ./scripts/deploy.sh --no-build       # Restart without rebuilding
#   ./scripts/deploy.sh --status         # Show service status + tunnel URL

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TUNNEL_FILE="$PROJECT_DIR/.tunnel-url"
LOG_PREFIX="[deploy]"

# ── Colors ─────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log()   { echo -e "${BLUE}${LOG_PREFIX}${NC} $*"; }
ok()    { echo -e "${GREEN}${LOG_PREFIX}${NC} $*"; }
warn()  { echo -e "${YELLOW}${LOG_PREFIX}${NC} $*"; }
err()   { echo -e "${RED}${LOG_PREFIX}${NC} $*" >&2; }

# ── Parse arguments ───────────────────────────────────────────────────────
NEW_TUNNEL=false
BACKEND_ONLY=false
FRONTEND_ONLY=false
NO_BUILD=false
STATUS_ONLY=false

for arg in "$@"; do
  case "$arg" in
    --new-tunnel)     NEW_TUNNEL=true ;;
    --backend-only)   BACKEND_ONLY=true ;;
    --frontend-only)  FRONTEND_ONLY=true ;;
    --no-build)       NO_BUILD=true ;;
    --status)         STATUS_ONLY=true ;;
    --help|-h)
      echo "Usage: $0 [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  --new-tunnel      Restart Cloudflare tunnel and display new URL"
      echo "  --backend-only    Rebuild and restart only the backend"
      echo "  --frontend-only   Rebuild and restart only the frontend"
      echo "  --no-build        Restart services without rebuilding images"
      echo "  --status          Show current service status and tunnel URL"
      echo "  -h, --help        Show this help"
      exit 0
      ;;
    *)
      err "Unknown option: $arg"
      exit 1
      ;;
  esac
done

cd "$PROJECT_DIR"

# ── Helpers ────────────────────────────────────────────────────────────────

check_prerequisites() {
  local missing=()
  command -v docker >/dev/null 2>&1 || missing+=("docker")
  command -v docker compose >/dev/null 2>&1 || {
    docker compose version >/dev/null 2>&1 || missing+=("docker compose")
  }

  if [[ ${#missing[@]} -gt 0 ]]; then
    err "Missing prerequisites: ${missing[*]}"
    exit 1
  fi

  if [[ ! -f .env ]]; then
    err ".env file not found. Copy from .env.example and configure:"
    err "  cp .env.example .env"
    exit 1
  fi
}

show_status() {
  echo ""
  log "Service Status:"
  echo "────────────────────────────────────────────────────────────"
  docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || true
  echo ""

  if [[ -f "$TUNNEL_FILE" ]]; then
    local url
    url=$(cat "$TUNNEL_FILE")
    echo -e "  ${CYAN}Tunnel URL:${NC} $url"
  fi

  # Check cloudflared
  if pgrep -x cloudflared >/dev/null 2>&1; then
    echo -e "  ${GREEN}Cloudflare Tunnel:${NC} running (PID $(pgrep -x cloudflared | head -1))"
  else
    echo -e "  ${YELLOW}Cloudflare Tunnel:${NC} not running"
  fi
  echo ""
}

wait_for_healthy() {
  local service="$1"
  local max_wait="${2:-120}"
  local elapsed=0

  log "Waiting for $service to be healthy..."
  while [[ $elapsed -lt $max_wait ]]; do
    local status
    status=$(docker inspect --format='{{.State.Health.Status}}' "monteweb-$service" 2>/dev/null || echo "missing")
    if [[ "$status" == "healthy" ]]; then
      ok "$service is healthy"
      return 0
    fi
    sleep 3
    elapsed=$((elapsed + 3))
    printf "\r  %s waiting... %ds/%ds" "$service" "$elapsed" "$max_wait"
  done
  echo ""
  err "$service did not become healthy within ${max_wait}s"
  docker logs "monteweb-$service" --tail 20 2>/dev/null || true
  return 1
}

# ── Cloudflare Tunnel ──────────────────────────────────────────────────────

install_cloudflared() {
  if command -v cloudflared >/dev/null 2>&1; then
    return 0
  fi

  log "Installing cloudflared..."
  local arch
  arch=$(uname -m)
  case "$arch" in
    x86_64)  arch="amd64" ;;
    aarch64) arch="arm64" ;;
    armv7l)  arch="arm" ;;
    *)       err "Unsupported architecture: $arch"; exit 1 ;;
  esac

  local url="https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-${arch}"
  if curl -fsSL "$url" -o /tmp/cloudflared; then
    sudo install -m 755 /tmp/cloudflared /usr/local/bin/cloudflared
    rm -f /tmp/cloudflared
    ok "cloudflared installed"
  else
    err "Failed to download cloudflared"
    exit 1
  fi
}

stop_tunnel() {
  if pgrep -x cloudflared >/dev/null 2>&1; then
    log "Stopping existing Cloudflare tunnel..."
    pkill -x cloudflared 2>/dev/null || true
    sleep 2
    # Force kill if still running
    pkill -9 -x cloudflared 2>/dev/null || true
    ok "Tunnel stopped"
  fi
}

start_tunnel() {
  install_cloudflared

  stop_tunnel

  local app_port
  app_port=$(grep -E '^APP_PORT=' .env 2>/dev/null | cut -d= -f2 | tr -d '[:space:]')
  app_port="${app_port:-80}"

  local target="http://localhost:${app_port}"
  local log_file="$PROJECT_DIR/.tunnel.log"

  log "Starting Cloudflare Quick Tunnel -> $target"
  cloudflared tunnel --url "$target" > "$log_file" 2>&1 &
  local pid=$!

  # Wait for tunnel URL to appear in logs
  local max_wait=30
  local elapsed=0
  local tunnel_url=""

  while [[ $elapsed -lt $max_wait ]]; do
    tunnel_url=$(grep -oP 'https://[a-z0-9-]+\.trycloudflare\.com' "$log_file" 2>/dev/null | head -1 || true)
    if [[ -n "$tunnel_url" ]]; then
      break
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done

  if [[ -z "$tunnel_url" ]]; then
    err "Could not extract tunnel URL after ${max_wait}s"
    err "Check log: $log_file"
    cat "$log_file" 2>/dev/null | tail -10
    return 1
  fi

  # Save and display
  echo "$tunnel_url" > "$TUNNEL_FILE"

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo -e "  ${GREEN}Cloudflare Tunnel URL:${NC}"
  echo -e "  ${CYAN}${tunnel_url}${NC}"
  echo ""
  echo -e "  PID: $pid"
  echo -e "  Log: $log_file"
  echo -e "  Saved to: $TUNNEL_FILE"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
}

# ── Main deployment ────────────────────────────────────────────────────────

deploy() {
  local start_time
  start_time=$(date +%s)

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo -e "  ${CYAN}MonteWeb Deployment${NC}"
  echo -e "  $(date '+%Y-%m-%d %H:%M:%S')"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""

  # Step 1: Build
  if [[ "$NO_BUILD" == false ]]; then
    if [[ "$BACKEND_ONLY" == true ]]; then
      log "Building backend..."
      docker compose build backend
    elif [[ "$FRONTEND_ONLY" == true ]]; then
      log "Building frontend..."
      docker compose build frontend
    else
      log "Building all services..."
      docker compose build backend frontend
    fi
    ok "Build complete"
  else
    log "Skipping build (--no-build)"
  fi

  # Step 2: Start/restart services
  if [[ "$BACKEND_ONLY" == true ]]; then
    log "Restarting backend..."
    docker compose up -d backend
    wait_for_healthy backend 120
  elif [[ "$FRONTEND_ONLY" == true ]]; then
    log "Restarting frontend..."
    docker compose up -d frontend
    wait_for_healthy frontend 30
  else
    log "Starting all services..."
    docker compose up -d
    wait_for_healthy postgres 60
    wait_for_healthy redis 30
    wait_for_healthy solr 60
    wait_for_healthy backend 120
    wait_for_healthy frontend 30
  fi

  ok "All services are up"

  # Step 3: Tunnel
  if [[ "$NEW_TUNNEL" == true ]]; then
    start_tunnel
  elif [[ -f "$TUNNEL_FILE" ]]; then
    echo ""
    echo -e "  ${CYAN}Tunnel URL (saved):${NC} $(cat "$TUNNEL_FILE")"
    if ! pgrep -x cloudflared >/dev/null 2>&1; then
      warn "Tunnel process is not running. Use --new-tunnel to start one."
    fi
  fi

  # Step 4: Summary
  local end_time elapsed_total
  end_time=$(date +%s)
  elapsed_total=$((end_time - start_time))

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo -e "  ${GREEN}Deployment complete${NC} (${elapsed_total}s)"
  echo ""

  # Show access URLs
  local app_port
  app_port=$(grep -E '^APP_PORT=' .env 2>/dev/null | cut -d= -f2 | tr -d '[:space:]')
  app_port="${app_port:-80}"

  if [[ "$app_port" == "80" ]]; then
    echo -e "  Local:  ${CYAN}http://localhost${NC}"
  else
    echo -e "  Local:  ${CYAN}http://localhost:${app_port}${NC}"
  fi

  if [[ -f "$TUNNEL_FILE" ]] && pgrep -x cloudflared >/dev/null 2>&1; then
    echo -e "  Tunnel: ${CYAN}$(cat "$TUNNEL_FILE")${NC}"
  fi

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
}

# ── Entry point ────────────────────────────────────────────────────────────

check_prerequisites

if [[ "$STATUS_ONLY" == true ]]; then
  show_status
  exit 0
fi

deploy
