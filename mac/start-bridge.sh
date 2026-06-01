#!/usr/bin/env zsh
set -euo pipefail

SCRIPT_DIR=${0:A:h}
APP_DIR="${PBI_APP_DIR:-$HOME/Library/Application Support/ParsecBubbleInput}"
NODE_BIN="${NODE:-$(command -v node || true)}"

if [[ -z "$NODE_BIN" ]]; then
  echo "Node.js is required. Install Node 20+ or set NODE=/path/to/node." >&2
  exit 1
fi

mkdir -p "$APP_DIR"
cp "$SCRIPT_DIR/server.js" "$APP_DIR/server.js"
if [[ -f "$SCRIPT_DIR/bridge.env" ]]; then
  cp "$SCRIPT_DIR/bridge.env" "$APP_DIR/bridge.env"
elif [[ ! -f "$APP_DIR/bridge.env" && -f "$SCRIPT_DIR/bridge.env.example" ]]; then
  cp "$SCRIPT_DIR/bridge.env.example" "$APP_DIR/bridge.env"
fi

if [[ -f "$APP_DIR/bridge.env" ]]; then
  set -a
  source "$APP_DIR/bridge.env"
  set +a
fi

touch "$APP_DIR/bridge.log"
chmod 700 "$APP_DIR"
chmod 600 "$APP_DIR"/.token "$APP_DIR/bridge.env" "$APP_DIR/bridge.log" 2>/dev/null || true

"$SCRIPT_DIR/stop-bridge.sh" >/dev/null 2>&1 || true

cd "$APP_DIR"
nohup "$NODE_BIN" server.js >> bridge.log 2>&1 &
echo $! > bridge.pid

sleep 1
curl -fsS --max-time 3 "http://localhost:${PBI_PORT:-8765}/api/health"
echo

if [[ -f "$APP_DIR/.token" ]]; then
  echo "Token file: $APP_DIR/.token"
  echo "Token: $(cat "$APP_DIR/.token")"
else
  echo "Token is configured through PBI_TOKEN / PARSEC_BUBBLE_TOKEN."
fi
